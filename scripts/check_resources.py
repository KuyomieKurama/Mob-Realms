#!/usr/bin/env python3
"""Check JSON syntax and translation parity without extra dependencies."""
import json
import pathlib
import re

root = pathlib.Path(__file__).resolve().parents[1]
resources = root / 'fabric-mod/src/main/resources'
for path in resources.rglob('*.json'):
    json.loads(path.read_text())
lang = resources / 'assets/mobrealms/lang'
en = json.loads((lang / 'en_us.json').read_text())
de = json.loads((lang / 'de_de.json').read_text())
assert en.keys() == de.keys(), 'Translation keys differ'
for key in en:
    assert len(re.findall(r'%s', en[key])) == len(re.findall(r'%s', de[key])), key
for path in (resources / 'data/mobrealms/mobrealms/species').glob('*.json'):
    assert 'species.mobrealms.' + path.stem in en, path
print(f'JSON valid; {len(en)} matching translation keys.')
