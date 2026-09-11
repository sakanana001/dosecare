"""Simpler global Featherline -> PicoHRT replacement for remaining string-bearing files."""
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

TARGETS = [
    'app/src/main/res/values/strings.xml',
    'app/src/main/res/values-b+zh+Hans/strings.xml',
    'app/src/main/res/values-b+zh+Hant/strings.xml',
    'app/src/main/res/values-b+yue+Hant+HK/strings.xml',
    'app/src/debug/res/values/strings.xml',
    'app/src/debug/res/values-b+zh+Hans/strings.xml',
    'fastlane/metadata/android/en-US/title.txt',
    'fastlane/metadata/android/en-US/full_description.txt',
    'fastlane/metadata/android/zh-CN/title.txt',
    'fastlane/metadata/android/zh-CN/full_description.txt',
]


def main():
    for rel in TARGETS:
        p = os.path.join(ROOT, rel)
        if not os.path.exists(p):
            print('SKIP missing:', rel)
            continue
        with open(p, 'r', encoding='utf-8') as f:
            content = f.read()
        before_count = content.count('Featherline')
        new = content.replace('Featherline', 'PicoHRT')
        after_count = new.count('Featherline')
        if new != content:
            with open(p, 'w', encoding='utf-8') as f:
                f.write(new)
            print(f'UPDATED: {rel} (replaced {before_count}, remaining {after_count})')
        else:
            print(f'NO CHANGE: {rel} (count={before_count})')


if __name__ == '__main__':
    main()
