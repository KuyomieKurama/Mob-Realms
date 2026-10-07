import argparse
import importlib.util
import json
import io
import tarfile
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('manager', ROOT / 'scripts/server_manager.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def jar(path, mod_id=None, version=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, 'w') as z:
        z.writestr('marker', 'fixture')
        if mod_id:
            z.writestr('fabric.mod.json', json.dumps({'id': mod_id, 'version': version,
                'depends': {'minecraft': m.VERSIONS['minecraft']}}))


class InstallerTests(unittest.TestCase):
    def setUp(self):
        parent = ROOT / 'build/installer-tests'
        parent.mkdir(parents=True, exist_ok=True)
        self.tmp = tempfile.TemporaryDirectory(dir=parent)
        self.root = Path(self.tmp.name)
        self.target = self.root / 'server with spaces'
        self.mod = self.root / 'mod.jar'
        jar(self.mod, 'mobrealms', m.VERSIONS['mobrealms'])

    def tearDown(self):
        self.tmp.cleanup()

    def args(self, **changes):
        values = dict(dir=str(self.target), mod=str(self.mod), xms='512M', xmx='1G', accept_eula=False)
        values.update(changes)
        return argparse.Namespace(**values)

    def fake_download(self, url, path):
        if 'fabric-api' in url:
            jar(path, 'fabric-api', m.VERSIONS['fabric_api'])
        else:
            jar(path)

    def fake_java(self, argv, **kwargs):
        if '-downloadMinecraft' in argv:
            stage = Path(argv[argv.index('-dir') + 1])
            jar(stage / 'server.jar')
            jar(stage / 'fabric-server-launch.jar')
            jar(stage / 'libraries/net/fabricmc/fabric-loader/loader.jar')
        else:
            stage = Path(kwargs['cwd'])
            (stage / 'eula.txt').write_text('eula=false\n')
            (stage / 'server.properties').write_text('online-mode=true\n')
        return subprocess.CompletedProcess(argv, 0)

    def install(self, **changes):
        with patch.object(m, 'java_binary', return_value='/fake/java'), \
             patch.object(m, 'check_memory'), \
             patch.object(m.shutil, 'disk_usage', return_value=type('Usage', (), {'free': 10 * 1024**3})()), \
             patch.object(m, 'verified_download', side_effect=self.fake_download), \
             patch.object(m.subprocess, 'run', side_effect=self.fake_java):
            return m.install(self.args(**changes))

    def test_install_and_check_preserve_eula(self):
        self.install()
        m.verify_install(self.target)
        self.assertFalse(m.eula_accepted(self.target))
        self.assertTrue((self.target / 'start-server.sh').stat().st_mode & 0o111)
        with patch.object(m, 'java_binary', return_value='/fake/java'), patch.object(m, 'check_memory'), patch.object(m.os, 'execv') as execute:
            m.start(argparse.Namespace(dir=str(self.target), check=True, xms=None, xmx=None))
            execute.assert_not_called()

    def test_existing_world_never_overwritten(self):
        self.target.mkdir()
        (self.target / 'world.dat').write_text('precious')
        with self.assertRaises(m.SetupError):
            self.install()
        self.assertEqual((self.target / 'world.dat').read_text(), 'precious')

    def test_rerun_preserves_world_and_settings(self):
        self.install()
        (self.target / 'world.dat').write_text('world')
        (self.target / 'server-memory.json').write_text('{"xms":"256M","xmx":"512M"}')
        self.install()
        self.assertEqual((self.target / 'world.dat').read_text(), 'world')
        self.assertEqual(json.loads((self.target / 'server-memory.json').read_text())['xmx'], '512M')

    def test_explicit_acceptance_only(self):
        self.install(accept_eula=True)
        self.assertTrue(m.eula_accepted(self.target))

    def test_missing_eula_blocks_exec(self):
        self.install()
        with patch.object(m, 'java_binary', return_value='/fake/java'), patch.object(m, 'check_memory'), patch.object(m.os, 'execv') as execute:
            with self.assertRaises(m.SetupError):
                m.start(argparse.Namespace(dir=str(self.target), check=False, xms=None, xmx=None))
            execute.assert_not_called()

    def test_corruption_blocks_verification(self):
        self.install()
        (self.target / 'mods/mob-realms.jar').write_bytes(b'broken')
        with self.assertRaises(m.SetupError):
            m.verify_install(self.target)

    def test_duplicate_mod_blocked(self):
        self.install()
        jar(self.target / 'mods/duplicate.jar', 'mobrealms', m.VERSIONS['mobrealms'])
        with self.assertRaises(m.SetupError):
            m.verify_install(self.target)

    def test_wrong_java_blocked(self):
        response = subprocess.CompletedProcess([], 0, 'java.specification.version = 21')
        with patch.object(m.shutil, 'which', return_value='/java'), patch.object(m.subprocess, 'run', return_value=response):
            with self.assertRaises(m.SetupError):
                m.java_binary()

    def test_memory_rejects_options_and_reversed_bounds(self):
        with self.assertRaises(m.SetupError):
            m.memory_bytes('1G -javaagent:bad.jar')
        with self.assertRaises(m.SetupError):
            m.check_memory('4G', '1G')

    def test_failed_download_leaves_no_installation(self):
        with patch.object(m, 'verified_download', side_effect=OSError('offline')):
            with patch.object(m, 'java_binary', return_value='/fake/java'), patch.object(m, 'check_memory'), \
                 patch.object(m.shutil, 'disk_usage', return_value=type('Usage', (), {'free': 10 * 1024**3})()):
                with self.assertRaises(OSError):
                    m.install(self.args())
        self.assertFalse(self.target.exists())
        self.assertFalse(list(self.root.glob('.mobrealms-install-*')))

    def test_checksum_mismatch_rejected(self):
        def download(url, path, limit=None):
            if url.endswith('.sha256'):
                path.write_text('0' * 64)
            else:
                path.write_bytes(b'wrong payload')
        with patch.object(m, 'download', side_effect=download):
            with self.assertRaises(m.SetupError):
                m.verified_download('https://example.invalid/file.jar', self.root / 'file.jar')

    def test_existing_java25_avoids_download(self):
        with patch.object(m, 'inspect_java', return_value='/existing/java'), patch.object(m, 'install_java25') as install:
            self.assertEqual(m.java_binary(auto_install=True), '/existing/java')
            install.assert_not_called()

    def test_missing_java_downloads_managed_jdk(self):
        home = self.root / 'jdk'
        with patch.object(m, 'inspect_java', return_value=None), \
             patch.object(m, 'java_home_directory', return_value=(home, 'x64')), \
             patch.object(m, 'install_java25', return_value=str(home / 'bin/java')) as install:
            self.assertEqual(m.java_binary(auto_install=True), str(home / 'bin/java'))
            install.assert_called_once_with(home, 'x64')

    def test_start_does_not_download(self):
        with patch.object(m, 'inspect_java', return_value=None), \
             patch.object(m, 'java_home_directory', return_value=(self.root / 'jdk', 'x64')), \
             patch.object(m, 'install_java25') as install:
            with self.assertRaises(m.SetupError):
                m.java_binary()
            install.assert_not_called()

    def test_archive_rejects_path_traversal(self):
        archive = self.root / 'bad.tar.gz'
        with tarfile.open(archive, 'w:gz') as tar:
            member = tarfile.TarInfo('../escaped')
            member.size = 1
            tar.addfile(member, io.BytesIO(b'x'))
        out = self.root / 'out'
        out.mkdir()
        with self.assertRaises(m.SetupError):
            m.unpack_jdk(archive, out)
        self.assertFalse((self.root / 'escaped').exists())

    def test_archive_rejects_external_symlink(self):
        archive = self.root / 'bad.tar.gz'
        with tarfile.open(archive, 'w:gz') as tar:
            member = tarfile.TarInfo('jdk/link')
            member.type = tarfile.SYMTYPE
            member.linkname = '../../escaped'
            tar.addfile(member)
        out = self.root / 'out'
        out.mkdir()
        with self.assertRaises(m.SetupError):
            m.unpack_jdk(archive, out)

    def test_managed_java_install_and_checksum(self):
        fixture = self.root / 'good.tar.gz'
        with tarfile.open(fixture, 'w:gz') as tar:
            for filename in ('jdk/bin/java', 'jdk/bin/javac'):
                member = tarfile.TarInfo(filename)
                member.mode = 0o755
                member.size = 1
                tar.addfile(member, io.BytesIO(b'x'))
        expected = m.digest(fixture)
        def download(url, target, limit=None):
            if 'api.adoptium.net' in url:
                target.write_text(json.dumps([{'version': {'major': 25}, 'release_name': 'fixture',
                    'binary': {'package': {'checksum': expected, 'link': 'https://example.invalid/jdk.tar.gz'}}}]))
            else:
                target.write_bytes(fixture.read_bytes())
        home = self.root / 'managed/jdk'
        with patch.object(m, 'download', side_effect=download), patch.object(m, 'inspect_java', return_value='/java'), \
             patch.object(m.shutil, 'disk_usage', return_value=type('Usage', (), {'free': 10 * 1024**3})()):
            self.assertEqual(m.install_java25(home, 'x64'), str(home / 'bin/java'))
        self.assertTrue((home / 'bin/javac').is_file())
        self.assertEqual(json.loads((home / 'mobrealms-java.json').read_text())['sha256'], expected)


if __name__ == '__main__':
    unittest.main()
