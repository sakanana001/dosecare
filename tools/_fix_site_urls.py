"""Wrap internal /download, /privacy, /zh-Hans/... hrefs with siteUrl().

Astro's `base: '/PicoHRT'` only auto-prefixes asset URLs (e.g. _astro/*.css)
and not the manual href="..." attributes the pages write. We rewrite
those manual hrefs to use the {siteUrl(...)} expression so the generated
HTML matches the GitHub Pages base path.

This script is a one-shot patcher for the initial scaffold; the converted
.astro files become the canonical sources afterwards.
"""
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'site'))
FILES = [
    'src/pages/index.astro',
    'src/pages/privacy.astro',
    'src/pages/download.astro',
    'src/pages/zh-Hans/index.astro',
    'src/pages/zh-Hans/privacy.astro',
    'src/pages/zh-Hans/download.astro',
]

# Paths that should be wrapped with siteUrl(). We are deliberately
# conservative: only touch href values that point at site pages, not at
# external sites, raw asset paths, or anchors.
HREF_PATTERN = re.compile(r'href="(/[^"#?]*)"')


def should_wrap(value: str) -> bool:
    """Only wrap paths that are clearly internal site pages."""
    if not value.startswith('/'):
        return False
    # Skip asset-y paths (likely already correct, or already prefixed).
    if value.startswith('/_astro/') or value.startswith('/_next/'):
        return False
    # Skip if the path already starts with the base.
    if value.startswith('/PicoHRT/') or value == '/PicoHRT':
        return False
    # Only wrap what looks like a page route.
    if value in ('/', '/download/', '/privacy/'):
        return True
    if value.startswith('/zh-Hans') or value.startswith('/zh-Hant') or value.startswith('/zh-CN'):
        return True
    return False


def patch_file(path: str) -> tuple[int, list[str]]:
    p = os.path.join(ROOT, path)
    with open(p, 'r', encoding='utf-8') as f:
        src = f.read()
    changes = []
    out = src

    def repl(match: re.Match) -> str:
        old = match.group(0)
        value = match.group(1)
        if not should_wrap(value):
            return old
        new = f'href={{siteUrl(\'{value}\')}}'
        changes.append(f'{value} -> siteUrl(\'{value}\')')
        return new

    new_src = HREF_PATTERN.sub(repl, src)
    if new_src != src:
        # Also need to import siteUrl at the top.
        if "from '../config'" not in new_src and "from '../../config'" not in new_src:
            if path.startswith('src/pages/zh-Hans/'):
                new_src = new_src.replace(
                    "import { getDictionary, type Locale } from '../../i18n/ui';",
                    "import { getDictionary, type Locale } from '../../i18n/ui';\nimport { siteUrl } from '../../config';",
                    1,
                )
            else:
                new_src = new_src.replace(
                    "import { getDictionary, type Locale } from '../i18n/ui';",
                    "import { getDictionary, type Locale } from '../i18n/ui';\nimport { siteUrl } from '../config';",
                    1,
                )
        with open(p, 'w', encoding='utf-8') as f:
            f.write(new_src)
    return (len(changes), changes)


def main():
    total = 0
    for rel in FILES:
        n, changes = patch_file(rel)
        print(f'{rel}: {n} href(s) wrapped')
        for c in changes:
            print(f'  - {c}')
        total += n
    print(f'Total: {total}')


if __name__ == '__main__':
    main()
