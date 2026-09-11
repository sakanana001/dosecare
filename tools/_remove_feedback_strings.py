"""Remove the Contact developer + Feedback string entries from strings.xml."""
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
TARGETS = [
    'app/src/main/res/values/strings.xml',
    'app/src/main/res/values-b+zh+Hans/strings.xml',
    'app/src/main/res/values-b+zh+Hant/strings.xml',
    'app/src/main/res/values-b+yue+Hant+HK/strings.xml',
]

KEYS = [
    'settings_about_contact_developer',
    'settings_about_feedback',
    'settings_about_feedback_summary',
    'settings_about_feedback_subject',
    'settings_about_feedback_body',
    'settings_about_feedback_chooser_title',
    'settings_about_feedback_no_email_app',
]

# Build a regex that matches the full <string ...>...</string> line, with
# optional leading whitespace, and removes it (including the trailing
# newline) so the surrounding block stays well-formed.
KEYS_ALT = '|'.join(KEYS)
LINE_PATTERN = re.compile(
    r'^[ \t]*<string\s+name="(?:' + KEYS_ALT + r')">.*?</string>[ \t]*\r?\n',
    re.MULTILINE,
)


def main() -> None:
    total = 0
    for rel in TARGETS:
        p = os.path.join(ROOT, rel)
        with open(p, 'r', encoding='utf-8') as f:
            content = f.read()
        new, n = LINE_PATTERN.subn('', content)
        if n:
            with open(p, 'w', encoding='utf-8') as f:
                f.write(new)
            print(f'{rel}: removed {n} line(s)')
            total += n
        else:
            print(f'{rel}: nothing to remove')
    print(f'Total: {total}')


if __name__ == '__main__':
    main()
