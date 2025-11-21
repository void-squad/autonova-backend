#!/usr/bin/env python3
import xml.etree.ElementTree as ET
from pathlib import Path

src = Path('test-results')
dst = Path('test-results-clean')
dst.mkdir(exist_ok=True)

for f in src.rglob('*'):
    if f.is_file() and f.suffix.lower() in ('.xml', '.trx'):
        try:
            ET.parse(f)
            target = dst / f.relative_to(src)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(f.read_bytes())
            print('Kept:', f)
        except Exception as e:
            print('Skipping invalid test file:', f, '->', e)
