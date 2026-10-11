"""DoseCare × PicoHRT 合并 — 第 0 期包名 rebrand。

把底座的 `com.picohrt` 改为 `com.dosecare.app`,同时修掉 benchmark 模块里
上一轮 rebrand 漏改的 `com.mkx.hrttracker`。

按字节做替换,完整保留 CRLF / LF 行尾与 BOM。

用法:
    python tools/_merge_rebrand_to_dosecare.py            # 只做 dry-run 报告
    python tools/_merge_rebrand_to_dosecare.py --apply    # 真正执行
"""

import os
import shutil
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

OLD_PKG = b'com.picohrt'
NEW_PKG = b'com.dosecare.app'
OLD_UPSTREAM = b'com.mkx.hrttracker'
NEW_UPSTREAM = b'com.dosecare.app'

# (源码目录, 旧包目录, 新包目录) —— 全部相对 ROOT
DIR_MOVES = [
    ('app/src/main/java/com', 'picohrt', 'dosecare/app'),
    ('app/src/test/java/com', 'picohrt', 'dosecare/app'),
    ('app/src/androidTest/java/com', 'picohrt', 'dosecare/app'),
    ('benchmark/src/main/java/com', 'mkx/hrttracker', 'dosecare/app'),
]

# 需要做文本替换的文件范围(相对 ROOT 的 glob 前缀)
TEXT_SCAN_DIRS = [
    'app/src',
    'benchmark/src',
]

# 只在这些文件里替换包名(其余文件如 docs/ README 留给后续人工处理)
TEXT_TARGET_SUFFIXES = ('.kt', '.xml', '.kts', '.pro')

SKIP_DIR_PARTS = ('.git', 'build', '.gradle', '.kotlin', '.idea')


def iter_target_files():
    for scan in TEXT_SCAN_DIRS:
        base = os.path.join(ROOT, scan)
        if not os.path.isdir(base):
            continue
        for dirpath, dirnames, filenames in os.walk(base):
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIR_PARTS]
            for name in filenames:
                if name.endswith(TEXT_TARGET_SUFFIXES):
                    yield os.path.join(dirpath, name)


def plan_dir_moves():
    moves = []
    for parent, old_rel, new_rel in DIR_MOVES:
        src = os.path.join(ROOT, parent, *old_rel.split('/'))
        dst = os.path.join(ROOT, parent, *new_rel.split('/'))
        if not os.path.isdir(src):
            continue
        if os.path.exists(dst):
            print(f'  SKIP (目标已存在): {parent}/{new_rel}')
            continue
        moves.append((src, dst))
    return moves


def main():
    apply = '--apply' in sys.argv

    print('=' * 68)
    print('目录移动计划')
    print('=' * 68)
    moves = plan_dir_moves()
    for src, dst in moves:
        rel_src = os.path.relpath(src, ROOT).replace('\\', '/')
        rel_dst = os.path.relpath(dst, ROOT).replace('\\', '/')
        count = sum(len(files) for _, _, files in os.walk(src))
        print(f'  {rel_src}')
        print(f'    -> {rel_dst}   ({count} 文件)')
    if not moves:
        print('  (无)')

    print()
    print('=' * 68)
    print('文本替换计划')
    print('=' * 68)
    hits = []
    for path in iter_target_files():
        with open(path, 'rb') as f:
            data = f.read()
        n_old = data.count(OLD_PKG)
        n_up = data.count(OLD_UPSTREAM)
        if n_old == 0 and n_up == 0:
            continue
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        hits.append((rel, n_old, n_up))
        print(f'  {rel}   com.picohrt×{n_old}  com.mkx.hrttracker×{n_up}')

    print()
    print(f'合计: {len(moves)} 个目录, {len(hits)} 个文件')
    total_subs = sum(n_old + n_up for _, n_old, n_up in hits)
    print(f'共 {total_subs} 处替换')

    if not apply:
        print()
        print('[dry-run] 加 --apply 执行实际修改')
        return

    print()
    print('=' * 68)
    print('执行')
    print('=' * 68)

    # 先做目录移动,再替换文本(替换只认包名字符串,不认路径,所以顺序无关,
    # 但先移动可以让后续的 git 状态更接近最终形态)
    for src, dst in moves:
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.move(src, dst)
        print(f'  MOVED  {os.path.relpath(src, ROOT)} -> {os.path.relpath(dst, ROOT)}')

    # 清掉搬空后的空壳目录
    for parent, old_rel, _ in DIR_MOVES:
        old_dir = os.path.join(ROOT, parent, *old_rel.split('/'))
        if os.path.isdir(old_dir) and not os.listdir(old_dir):
            os.rmdir(old_dir)
            print(f'  RMDIR  {os.path.relpath(old_dir, ROOT)}')

    for path in iter_target_files():
        with open(path, 'rb') as f:
            data = f.read()
        new = data.replace(OLD_PKG, NEW_PKG).replace(OLD_UPSTREAM, NEW_UPSTREAM)
        if new != data:
            with open(path, 'wb') as f:
                f.write(new)
            print(f'  PATCH  {os.path.relpath(path, ROOT)}')

    print()
    print('完成。下一步: 手动处理 strings.xml 的 app_name 显示名。')


if __name__ == '__main__':
    main()
