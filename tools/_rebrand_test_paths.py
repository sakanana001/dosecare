"""Rebrand hardcoded source file paths in test files.

The previous PR (the package rename) moved all source files from
src/main/java/com/mkx/hrttracker/ to src/main/java/com/picohrt/, but
several test files reference those source files via string literal paths
when they want to read and assert on the file content. This script
rewrites those string literals so the tests can still find the files.
"""
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

# Old path -> new path
OLD = 'app/src/main/java/com/mkx/hrttracker/'
NEW = 'app/src/main/java/com/picohrt/'

count_updated = 0
count_total_subs = 0
for dirpath, dirnames, filenames in os.walk(os.path.join(ROOT, 'app/src/test')):
    for name in filenames:
        if not name.endswith('.kt'):
            continue
        p = os.path.join(dirpath, name)
        try:
            with open(p, 'r', encoding='utf-8') as f:
                content = f.read()
        except UnicodeDecodeError:
            continue
        n = content.count(OLD)
        if n == 0:
            continue
        new = content.replace(OLD, NEW)
        with open(p, 'w', encoding='utf-8') as f:
            f.write(new)
        rel = os.path.relpath(p, ROOT)
        print(f'UPDATED ({n}x): {rel}')
        count_updated += 1
        count_total_subs += n

# Also fix the app/src/androidTest if any
for dirpath, dirnames, filenames in os.walk(os.path.join(ROOT, 'app/src/androidTest')):
    for name in filenames:
        if not name.endswith('.kt'):
            continue
        p = os.path.join(dirpath, name)
        try:
            with open(p, 'r', encoding='utf-8') as f:
                content = f.read()
        except UnicodeDecodeError:
            continue
        n = content.count(OLD)
        if n == 0:
            continue
        new = content.replace(OLD, NEW)
        with open(p, 'w', encoding='utf-8') as f:
            f.write(new)
        rel = os.path.relpath(p, ROOT)
        print(f'UPDATED ({n}x): {rel}')
        count_updated += 1
        count_total_subs += n

print(f'Total: {count_updated} files, {count_total_subs} substitutions')
