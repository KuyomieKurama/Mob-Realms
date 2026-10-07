#!/usr/bin/env python3
"""Install/check/start a pinned Mob Realms server. Python standard library only."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile

VERSIONS = {"minecraft": "26.3", "loader": "0.19.5", "fabric_api": "0.161.0+26.3",
            "installer": "1.1.2", "mobrealms": "0.1.0-dev"}
MANIFEST = "mobrealms-install.json"
MAVEN = "https://maven.fabricmc.net"
EULA_URL = "https://www.minecraft.net/eula"


class SetupError(Exception):
    pass


def require(condition, message):
    if not condition:
        raise SetupError(message)


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def java_binary():
    choice = os.environ.get('JAVA_BIN')
    if not choice:
        choice = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else 'java'
    java = shutil.which(choice)
    require(java, 'Java fehlt / Java missing. Select Java 25 with JAVA_HOME or JAVA_BIN.')
    result = subprocess.run([java, '-XshowSettings:properties', '-version'], text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=20)
    match = re.search(r'java.specification.version\s*=\s*(\d+)', result.stdout)
    require(result.returncode == 0 and match and int(match.group(1)) == 25,
            'Java 25 erforderlich / Java 25 required. Check JAVA_HOME/JAVA_BIN and java -version.')
    return java


def memory_bytes(value):
    match = re.fullmatch(r'([1-9][0-9]*)([MmGg])', value)
    require(match, 'RAM must use a positive integer and M/G, e.g. 1024M or 4G.')
    return int(match.group(1)) * (1024 ** (2 if match.group(2).lower() == 'm' else 3))


def check_memory(xms, xmx):
    minimum, maximum = memory_bytes(xms), memory_bytes(xmx)
    require(minimum <= maximum, 'Xms must not exceed Xmx.')
    require(minimum >= 256 * 1024 ** 2, 'Xms must be at least 256M.')
    if Path('/proc/meminfo').exists():
        text = Path('/proc/meminfo').read_text()
        match = re.search(r'^MemTotal:\s+(\d+) kB', text, re.M)
        if match:
            require(maximum + 256 * 1024 ** 2 <= int(match.group(1)) * 1024,
                    'Insufficient physical RAM for Xmx plus 256M reserve. Reduce --xmx/--xms.')
    # Also respect explicit cgroup memory limits, where present.
    for filename in ('/sys/fs/cgroup/memory.max', '/sys/fs/cgroup/memory/memory.limit_in_bytes'):
        p = Path(filename)
        if p.exists():
            value = p.read_text().strip()
            if value.isdigit():
                require(maximum + 256 * 1024 ** 2 <= int(value), 'Container RAM limit too low for Xmx. Reduce --xmx.')


def read_mod(path):
    try:
        with zipfile.ZipFile(path) as jar:
            metadata = jar.getinfo('fabric.mod.json')
            require(metadata.file_size <= 1024 * 1024, f'Oversized mod metadata: {path.name}')
            data = json.loads(jar.read(metadata))
            require(isinstance(data, dict) and isinstance(data.get('id'), str), f'Invalid mod metadata: {path.name}')
            return data
    except (zipfile.BadZipFile, KeyError, json.JSONDecodeError) as ex:
        raise SetupError(f'Invalid Fabric mod JAR / Ungültige Mod-JAR: {path}') from ex


def check_mod(path, mod_id, version):
    data = read_mod(path)
    require(data.get('id') == mod_id and data.get('version') == version,
            f'{path.name}: expected {mod_id} {version}, found {data.get("id")} {data.get("version")}.')
    if mod_id == 'mobrealms':
        require(data.get('depends', {}).get('minecraft') == VERSIONS['minecraft'],
                'Mob Realms JAR does not declare the required Minecraft version.')
    return data


def download(url, destination, limit=512 * 1024 ** 2):
    require(url.startswith('https://'), 'HTTPS required.')
    request = urllib.request.Request(url, headers={'User-Agent': 'Mob-Realms-Server-Setup/0.1'})
    with urllib.request.urlopen(request, timeout=60) as response, destination.open('wb') as out:
        require(response.url.startswith('https://'), 'Refusing insecure download redirect.')
        size = 0
        while block := response.read(1024 * 1024):
            size += len(block)
            require(size <= limit, f'Download too large: {url}')
            out.write(block)


def verified_download(url, destination):
    checksum = destination.with_suffix(destination.suffix + '.sha256')
    try:
        download(url + '.sha256', checksum, 4096)
        expected = checksum.read_text().strip().split()[0]
        require(re.fullmatch(r'[0-9a-fA-F]{64}', expected), 'Invalid upstream SHA-256 checksum.')
        download(url, destination)
        require(digest(destination) == expected.lower(), f'Download checksum mismatch: {destination.name}')
        require(zipfile.is_zipfile(destination), f'Download is not a JAR: {destination.name}')
    finally:
        checksum.unlink(missing_ok=True)


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + '\n')


def eula_accepted(directory):
    path = directory / 'eula.txt'
    if not path.is_file():
        return False
    values = re.findall(r'^\s*eula\s*=\s*(true|false)\s*$', path.read_text(), re.M)
    return bool(values) and values[-1] == 'true'


def accept_eula(directory):
    # Called ONLY in response to the explicit --accept-eula option.
    (directory / 'eula.txt').write_text(f'# Accepted by explicit --accept-eula option; {EULA_URL}\neula=true\n')


def verify_install(directory):
    manifest_path = directory / MANIFEST
    require(manifest_path.is_file(), f'Installation manifest missing: {manifest_path}')
    manifest = json.loads(manifest_path.read_text())
    require(manifest.get('schema') == 1 and manifest.get('versions') == VERSIONS, 'Installation version mismatch; use a new directory.')
    files = manifest.get('files')
    require(isinstance(files, dict) and files, 'Empty or invalid installation manifest.')
    required = {'server.jar', 'fabric-server-launch.jar', 'mods/mob-realms.jar', 'mods/fabric-api.jar'}
    require(required.issubset(files), 'Installation manifest omits required files.')
    require(any(p.startswith('libraries/') and 'fabric-loader' in p for p in files), 'Fabric Loader libraries missing from manifest.')
    for relative, expected in files.items():
        path = directory / relative
        require(not Path(relative).is_absolute() and '..' not in Path(relative).parts, 'Invalid manifest path.')
        require(path.is_file() and not path.is_symlink(), f'Missing or symlinked runtime file: {relative}')
        require(digest(path) == expected, f'File changed/corrupt: {relative}. Restore the file or install into a new directory.')
    check_mod(directory / 'mods/mob-realms.jar', 'mobrealms', VERSIONS['mobrealms'])
    check_mod(directory / 'mods/fabric-api.jar', 'fabric-api', VERSIONS['fabric_api'])
    seen = set()
    for path in sorted((directory / 'mods').glob('*.jar')):
        data = read_mod(path)
        require(data['id'] not in seen, f'Duplicate mod ID: {data["id"]}; remove the duplicate JAR.')
        require(data.get('environment') != 'client', f'Client-only mod on server: {path.name}')
        seen.add(data['id'])
    return manifest


def install(args):
    directory = Path(args.dir).expanduser().absolute()
    require(not directory.is_symlink(), 'Server target must not be a symlink.')
    java = java_binary()
    check_memory(args.xms, args.xmx)
    if (directory / MANIFEST).is_file():
        verify_install(directory)
        print('Installation already valid; world, mods and settings preserved.')
        if args.accept_eula:
            accept_eula(directory)
        return directory
    require(not directory.exists() or (directory.is_dir() and not any(directory.iterdir())),
            'Target directory is not empty. Use a new directory; existing servers are never overwritten.')
    script_dir = Path(__file__).resolve().parent
    repo = script_dir.parent
    mod = Path(args.mod).expanduser().absolute() if args.mod else repo / f'fabric-mod/build/libs/mob-realms-{VERSIONS["mobrealms"]}.jar'
    if not mod.is_file() and not args.mod:
        require((repo / 'gradlew').is_file(), 'No local Mod JAR. Supply --mod /path/to/mob-realms.jar.')
        print('Building Mob Realms and running core tests with Gradle …', flush=True)
        env = dict(os.environ)
        env['JAVA_HOME'] = str(Path(java).resolve().parent.parent)
        subprocess.run(['bash', './gradlew', '--no-daemon', 'build'], cwd=repo, env=env, check=True)
    require(mod.is_file(), f'Mod JAR not found: {mod}')
    check_mod(mod, 'mobrealms', VERSIONS['mobrealms'])
    directory.parent.mkdir(parents=True, exist_ok=True)
    require(shutil.disk_usage(directory.parent).free >= 2 * 1024 ** 3, 'At least 2 GiB free disk space required for installation.')
    stage = Path(tempfile.mkdtemp(prefix='.mobrealms-install-', dir=directory.parent))
    try:
        installer = stage / 'fabric-installer.jar'
        url = f'{MAVEN}/net/fabricmc/fabric-installer/{VERSIONS["installer"]}/fabric-installer-{VERSIONS["installer"]}.jar'
        print('Downloading and verifying Fabric installer …', flush=True)
        verified_download(url, installer)
        subprocess.run([java, '-jar', str(installer), 'server', '-mcversion', VERSIONS['minecraft'],
                        '-loader', VERSIONS['loader'], '-downloadMinecraft', '-dir', str(stage)], check=True, timeout=600)
        installer.unlink()
        (stage / 'mods').mkdir(exist_ok=True)
        api = VERSIONS['fabric_api']
        verified_download(f'{MAVEN}/net/fabricmc/fabric-api/fabric-api/{api}/fabric-api-{api}.jar', stage / 'mods/fabric-api.jar')
        shutil.copyfile(mod, stage / 'mods/mob-realms.jar')
        # Only a settings bootstrap: does not accept the EULA and does not load a world.
        subprocess.run([java, '-jar', str(stage / 'fabric-server-launch.jar'), '--initSettings'],
                       cwd=stage, check=True, timeout=180)
        # Hash installed runtime files after bootstrap has resolved/extracted its libraries.
        tracked = [p for p in stage.rglob('*') if p.is_file() and
                   ((p.suffix == '.jar' and (p.parent == stage or p.relative_to(stage).parts[0] in ('libraries', 'versions', 'mods')))
                    or p.name == 'fabric-server-launcher.properties')]
        manifest = {'schema': 1, 'versions': VERSIONS,
                    'files': {p.relative_to(stage).as_posix(): digest(p) for p in tracked}}
        write_json(stage / MANIFEST, manifest)
        write_json(stage / 'server-memory.json', {'xms': args.xms, 'xmx': args.xmx})
        shutil.copyfile(script_dir / 'server_manager.py', stage / 'server_manager.py')
        shutil.copyfile(script_dir / 'start-server.sh', stage / 'start-server.sh')
        (stage / 'start-server.sh').chmod(0o755)
        if args.accept_eula:
            accept_eula(stage)
        verify_install(stage)
        # Rename only into a fresh/empty target. Never remove an existing world's contents.
        if directory.exists():
            directory.rmdir()
        stage.rename(directory)
    finally:
        if stage.exists():
            shutil.rmtree(stage)
    print(f'Installed / Installiert: {directory}\nMods: {directory / "mods"}')
    print(f'Read {EULA_URL}; then set eula=true in eula.txt or use explicit --accept-eula.')
    print(f'Start: {directory / "start-server.sh"}')
    return directory


def start(args):
    directory = Path(args.dir).expanduser().resolve()
    java = java_binary()
    verify_install(directory)
    memory = json.loads((directory / 'server-memory.json').read_text())
    xms, xmx = args.xms or memory['xms'], args.xmx or memory['xmx']
    check_memory(xms, xmx)
    require(os.access(directory, os.W_OK), 'Server directory is not writable.')
    if args.check:
        print('Dependency/integrity checks passed. EULA: ' + ('accepted' if eula_accepted(directory) else 'not accepted; start blocked'))
        return
    require(eula_accepted(directory), f'EULA not accepted. Read {EULA_URL} and set eula=true in {directory / "eula.txt"}.')
    # Prevent a second managed server process in this directory; lock survives exec.
    import fcntl
    lock = os.open(directory / '.mobrealms-server.lock', os.O_CREAT | os.O_RDWR, 0o600)
    try:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError as ex:
        os.close(lock)
        raise SetupError('A managed server is already running in this directory.') from ex
    os.set_inheritable(lock, True)
    os.chdir(directory)
    print(f'Checks passed. Starting Minecraft {VERSIONS["minecraft"]} / Fabric {VERSIONS["loader"]} …', flush=True)
    os.execv(java, [java, f'-Xms{xms}', f'-Xmx{xmx}', '-jar', 'fabric-server-launch.jar', 'nogui'])


def main():
    require(sys.version_info >= (3, 10), 'Python 3.10+ required.')
    parser = argparse.ArgumentParser(description='Mob Realms Linux server installer and dependency checks')
    commands = parser.add_subparsers(dest='command', required=True)
    setup = commands.add_parser('install')
    setup.add_argument('--dir', default='mob-realms-server', help='New/empty server directory')
    setup.add_argument('--mod', help='Built Mob Realms JAR; otherwise build the repository if needed')
    setup.add_argument('--xms', default='1G')
    setup.add_argument('--xmx', default='4G')
    setup.add_argument('--accept-eula', action='store_true', help=f'Explicitly accept {EULA_URL}')
    setup.add_argument('--start', action='store_true', help='Start after installation if all checks and EULA pass')
    launch = commands.add_parser('start')
    launch.add_argument('--dir', required=True)
    launch.add_argument('--check', action='store_true', help='Validate dependencies/integrity without starting')
    launch.add_argument('--xms')
    launch.add_argument('--xmx')
    args = parser.parse_args()
    if args.command == 'install':
        directory = install(args)
        if args.start:
            start(argparse.Namespace(dir=str(directory), check=False, xms=None, xmx=None))
    else:
        start(args)


if __name__ == '__main__':
    try:
        main()
    except (SetupError, OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
        print(f'Fehler / Error: {error}', file=sys.stderr)
        sys.exit(1)
