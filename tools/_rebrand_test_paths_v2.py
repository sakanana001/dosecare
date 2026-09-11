"""Fix the remaining relative 'src/main/java/com/mkx/hrttracker/...' references in test files.

The earlier script handled the 'app/src/main/java/com/...' absolute-form paths.
These references use the shorter 'src/main/java/...' form because the tests
run with `app/` as the working directory.
"""
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

# These are relative-to-app/ paths that the test framework resolves at
# runtime with the app module as cwd.
OLD = 'src/main/java/com/mkx/hrttracker/'
NEW = 'src/main/java/com/picohrt/'

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

print(f'Total: {count_updated} files, {count_total_subs} substitutions')
