#!/usr/bin/env bash
set -euo pipefail
command -v python3 >/dev/null 2>&1 || { echo 'Fehler: Python 3.10+ fehlt. / Error: Python 3.10+ required.' >&2; exit 1; }
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "$SCRIPT_DIR/server_manager.py" start --dir "$SCRIPT_DIR" "$@"
