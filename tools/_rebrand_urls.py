"""Bulk rewrite mkx173/Featherline -> picohrt/PicoHRT in non-CHANGELOG docs.

For CHANGELOG.md, the historical entries reference upstream tags / commits
that this fork does not have, so we leave the historical CHANGELOG URLs
alone (they're truthful at the time of writing) and rely on the
'v0.6.0 and earlier describe upstream history' note in the CHANGELOG
preamble.

Also avoid the dependency-license-report.html which is auto-generated.
"""
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))


def main():
    failures = 0
    for dirpath, dirnames, filenames in os.walk(ROOT):
        # Skip build outputs and the auto-generated license HTML
        rel_dir = os.path.relpath(dirpath, ROOT)
        parts = rel_dir.split(os.sep)
        if any(p in parts for p in ('build', '.git', 'gradle', 'app/build', 'tools/_rebrand_scripts_archive')):
            continue
        if rel_dir.startswith('docs/generated'):
            continue
        for name in filenames:
            if not name.endswith(('.md', '.kts', '.kt', '.xml', '.txt')):
                continue
            p = os.path.join(dirpath, name)
            rel = os.path.relpath(p, ROOT)
            if 'CHANGELOG' in name:
                continue
            try:
                with open(p, 'r', encoding='utf-8') as f:
                    content = f.read()
            except UnicodeDecodeError:
                continue
            new = content.replace('mkx173/Featherline', 'picohrt/PicoHRT')
            if new != content:
                with open(p, 'w', encoding='utf-8') as f:
                    f.write(new)
                print(f'UPDATED: {rel}')
    print('done')


if __name__ == '__main__':
    main()
