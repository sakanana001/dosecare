"""第 0 期: 把 App 显示名从 PicoHRT 改为 DoseCare(全语言 + debug 变体)。

只改 app_name,不动其它字符串里的品牌词(那些属于内容文案,第 4 期统一处理)。
"""

import glob
import io
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

OLD_NAME = 'PicoHRT'
NEW_NAME = 'DoseCare'

APPLY = '--apply' in sys.argv


def main():
    targets = sorted(glob.glob(os.path.join(ROOT, 'app/src/*/res/values*/strings.xml')))
    print(f'找到 {len(targets)} 个 strings.xml')

    pending = []
    for path in targets:
        with io.open(path, encoding='utf-8') as f:
            content = f.read()
        m = re.search(r'<string name="app_name">(.*?)</string>', content)
        if not m:
            continue
        current = m.group(1)
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        print(f'  {rel}  app_name = "{current}"')
        if current == NEW_NAME:
            continue
        new_content = content.replace(
            f'<string name="app_name">{current}</string>',
            f'<string name="app_name">{NEW_NAME}</string>',
        )
        pending.append((path, rel, current, new_content))

    if not APPLY:
        print()
        print(f'[dry-run] 将修改 {len(pending)} 个文件,加 --apply 执行')
        return

    for path, rel, current, new_content in pending:
        with io.open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(new_content)
        print(f'  PATCH  {rel}  "{current}" -> "{NEW_NAME}"')

    print()
    print(f'完成,共修改 {len(pending)} 个文件。')


if __name__ == '__main__':
    main()
