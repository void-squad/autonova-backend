#!/usr/bin/env python3
import xml.etree.ElementTree as ET
from pathlib import Path

def root_tag_name(elem):
    # strip namespace if present
    tag = elem.tag
    if '}' in tag:
        return tag.split('}', 1)[1]
    return tag

VALID_ROOTS = {'testsuite', 'testsuites', 'TestRun'}

src = Path('test-results')
dst = Path('test-results-clean')
dst.mkdir(exist_ok=True)

for f in src.rglob('*'):
    if f.is_file() and f.suffix.lower() in ('.xml', '.trx'):
        try:
            tree = ET.parse(f)
            root = tree.getroot()
            tag = root_tag_name(root)
            if tag not in VALID_ROOTS:
                print('Skipping non-junit/trx file (root="%s"): %s' % (tag, f))
                continue
            target = dst / f.relative_to(src)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(f.read_bytes())
            print('Kept:', f)
        except Exception as e:
            print('Skipping invalid test file:', f, '->', e)
