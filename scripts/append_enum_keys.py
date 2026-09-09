"""Append enum i18n keys to all 3 strings.xml files."""
import os, re

# Read generated keys
with open('scripts/enum_keys.txt', 'r', encoding='utf-16') as f:
    lines = [l.strip() for l in f if l.strip() and not l.startswith('#')]

# Parse: key|zh|en|ja
keys = []
for line in lines:
    parts = line.split('|')
    if len(parts) == 4:
        keys.append((parts[0], parts[1], parts[2], parts[3]))

print(f'Loaded {len(keys)} keys')

# Build section blocks for each language
# zh uses key=zh (or key=fallback en for indication where zh is null)
# en/ja use their respective column

zh_lines = []
en_lines = []
ja_lines = []
for k, zh, en, ja in keys:
    # For ind_* keys, zh might be null; fall back to en
    zh_val = zh if zh else en
    zh_esc = zh_val.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    en_esc = en.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    ja_esc = ja.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    zh_lines.append(f'    <string name="{k}">{zh_esc}</string>')
    en_lines.append(f'    <string name="{k}">{en_esc}</string>')
    ja_lines.append(f'    <string name="{k}">{ja_esc}</string>')

# Group keys by section for readability
def group_keys(prefix, lines):
    """Yield (group_label, [lines]) tuples for keys starting with prefix."""
    matching = [l for l in lines if f'name="{prefix}_' in l or f'name="{prefix}"' in l]
    return matching

# Simple: dump all under one section header
def make_block(section_label, lines):
    return ['', f'    <!-- ====== {section_label} ====== -->'] + lines

# Group by key prefix
def categorize_key(k):
    if k.startswith('drug_cat_'): return 'DrugCategory enum displayName (33 values × 3 lang)'
    if k.startswith('ind_'): return 'IndicationGroup enum displayName (43 values × 3 lang)'
    if k.startswith('sev_'): return 'OverdoseSeverity enum displayName (4 values × 3 lang)'
    if k.startswith('cyp_'): return 'CypEnzyme enum displayName (8 values × 3 lang)'
    if k.startswith('pathway_'): return 'PathwayType enum displayName (11 values × 3 lang)'
    return 'Other'

groups_zh = {}
groups_en = {}
groups_ja = {}
for k, zh, en, ja in keys:
    cat = categorize_key(k)
    zh_esc = (zh if zh else en).replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    en_esc = en.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    ja_esc = ja.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    groups_zh.setdefault(cat, []).append(f'    <string name="{k}">{zh_esc}</string>')
    groups_en.setdefault(cat, []).append(f'    <string name="{k}">{en_esc}</string>')
    groups_ja.setdefault(cat, []).append(f'    <string name="{k}">{ja_esc}</string>')

# Build the trailing block
def build_trailing(groups):
    out = []
    for cat, ls in groups.items():
        out.append('')
        out.append(f'    <!-- ====== {cat} ====== -->')
        out.extend(ls)
    return '\n'.join(out) + '\n'

# Find </resources> and insert before it
for path, groups in [
    ('app/src/main/res/values/strings.xml', groups_zh),
    ('app/src/main/res/values-en/strings.xml', groups_en),
    ('app/src/main/res/values-ja/strings.xml', groups_ja),
]:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    block = build_trailing(groups)
    # Insert before </resources>
    new_content = content.replace('\n</resources>', '\n' + block + '\n</resources>')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    added = sum(len(v) for v in groups.values())
    print(f'{path}: added {added} strings')
