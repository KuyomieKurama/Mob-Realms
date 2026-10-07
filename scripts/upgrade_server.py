#!/usr/bin/env python3
"""Upgrade a stopped managed server, preserving a complete sibling backup."""
import argparse
import contextlib
import fcntl
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time
import server_manager as sm

JOURNAL = 'mobrealms-upgrade-incomplete.json'


@contextlib.contextmanager
def stopped_server(directory):
    """Hold both the managed process lock and Minecraft's POSIX world locks."""
    with contextlib.ExitStack() as stack:
        managed = stack.enter_context((directory / '.mobrealms-server.lock').open('a+b'))
        try:
            fcntl.flock(managed, fcntl.LOCK_EX | fcntl.LOCK_NB)
            for path in directory.rglob('session.lock'):
                sm.require(not path.is_symlink(), 'Symlinked world lock is unsupported.')
                lock = stack.enter_context(path.open('r+b'))
                fcntl.lockf(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as ex:
            raise sm.SetupError('Stop the Minecraft server with stop before upgrading.') from ex
        yield


def replace_file(source, destination):
    fd, name = tempfile.mkstemp(prefix='.upgrade-', dir=destination.parent)
    try:
        with os.fdopen(fd, 'wb') as out, source.open('rb') as src:
            shutil.copyfileobj(src, out); out.flush(); os.fsync(out.fileno())
        os.chmod(name, source.stat().st_mode & 0o777)
        os.replace(name, destination)
    finally:
        if os.path.exists(name):
            os.unlink(name)


def upgrade(directory, mod):
    directory = Path(directory).expanduser().absolute()
    mod = Path(mod).expanduser().resolve()
    sm.require(directory.is_dir() and not directory.is_symlink(), 'Managed server directory required.')
    sm.check_mod(mod, 'mobrealms', sm.VERSIONS['mobrealms'])
    # Reject linked worlds/runtime files rather than creating an incomplete backup outside this tree.
    sm.require(not any(p.is_symlink() for p in directory.rglob('*')), 'Symlinks in server directory are unsupported for automatic backup.')
    with stopped_server(directory):
        previous = json.loads((directory / sm.MANIFEST).read_text())
        versions = previous.get('versions', {})
        sm.require(all(versions.get(k) == v for k, v in sm.VERSIONS.items() if k != 'mobrealms'),
                   'Minecraft/Fabric upgrades require a separate installation; only the mod is upgraded here.')
        sm.require(versions.get('mobrealms') in ('0.1.0-dev', '0.2.0-dev', '0.5.0-dev', '0.6.0-dev', sm.VERSIONS['mobrealms']), 'Unsupported source mod version.')
        sm.verify_install(directory, versions)
        if sm.digest(mod) == sm.digest(directory / 'mods/mob-realms.jar'):
            print('Identical mod already installed; no changes.'); return None
        size = sum(p.stat().st_size for p in directory.rglob('*') if p.is_file())
        sm.require(shutil.disk_usage(directory.parent).free > size + mod.stat().st_size * 2 + 64 * 1024**2,
                   'Insufficient space for full server backup and upgrade.')
        backup = directory.with_name(directory.name + '.backup-' + time.strftime('%Y%m%d-%H%M%S') + '-' + str(time.time_ns() % 1000000))
        shutil.copytree(directory, backup)
        print(f'Full backup / Vollständiges Backup: {backup}', flush=True)
        sm.write_json(directory / JOURNAL, {'backup': str(backup), 'target': sm.VERSIONS['mobrealms']})
        replacements = ['mods/mob-realms.jar', 'server_manager.py', 'start-server.sh', sm.MANIFEST]
        try:
            scripts = Path(__file__).resolve().parent
            replace_file(scripts / 'server_manager.py', directory / 'server_manager.py')
            replace_file(scripts / 'start-server.sh', directory / 'start-server.sh')
            (directory / 'start-server.sh').chmod(0o755)
            replace_file(mod, directory / replacements[0])
            current = dict(previous)
            current['versions'] = dict(sm.VERSIONS)
            current['files'] = dict(previous['files'])
            current['files']['mods/mob-realms.jar'] = sm.digest(directory / 'mods/mob-realms.jar')
            # Reuse atomic replacement; leave the journal until all replacements are complete.
            with tempfile.TemporaryDirectory(dir=directory.parent) as tmp:
                manifest = Path(tmp) / sm.MANIFEST; sm.write_json(manifest, current)
                replace_file(manifest, directory / sm.MANIFEST)
            (directory / JOURNAL).unlink()
            sm.verify_install(directory)
        except Exception:
            # No world was opened. Restore every changed file; keep backup for manual recovery.
            for relative in replacements:
                replace_file(backup / relative, directory / relative)
            (directory / JOURNAL).unlink(missing_ok=True)
            raise
        print('Upgrade complete. World/settings preserved. Server remains stopped.')
        print('Update the client to the same Mob Realms JAR before reconnecting.')
        return backup


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dir', required=True, help='Stopped server created by install-server.sh')
    parser.add_argument('--mod', help='New built mod JAR; omitted: build current repository with Java 25')
    args = parser.parse_args()
    java = sm.java_binary(auto_install=True, require_jdk=not args.mod)
    repo = Path(__file__).resolve().parent.parent
    if args.mod:
        mod = Path(args.mod)
    else:
        env = dict(os.environ); env['JAVA_HOME'] = str(Path(java).resolve().parent.parent)
        subprocess.run(['bash', './gradlew', '--no-daemon', 'build'], cwd=repo, env=env, check=True)
        mod = repo / f'fabric-mod/build/libs/mob-realms-{sm.VERSIONS["mobrealms"]}.jar'
    upgrade(args.dir, mod)


if __name__ == '__main__':
    try:
        main()
    except (sm.SetupError, OSError, ValueError, KeyError, subprocess.SubprocessError) as ex:
        print(f'Fehler / Error: {ex}', file=sys.stderr); sys.exit(1)
