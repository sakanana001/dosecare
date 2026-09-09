"""Remove duplicate <string> entries in all 3 strings.xml files."""
import re

for path in [
    'app/src/main/res/values/strings.xml',
    'app/src/main/res/values-en/strings.xml',
    'app/src/main/res/values-ja/strings.xml',
]:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    seen = set()
    out_lines = []
    for line in content.split('\n'):
        m = re.match(r'^\s*<string name="([^"]+)"', line)
        if m:
            key = m.group(1)
            if key in seen:
                # Skip duplicate
                continue
            seen.add(key)
        out_lines.append(line)
    new_content = '\n'.join(out_lines)
    if new_content != content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'{path}: deduped, {len(seen)} unique keys')
    else:
        print(f'{path}: no change')
