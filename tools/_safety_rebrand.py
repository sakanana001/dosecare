"""Update remaining Featherline mentions in safety.md and other narrative docs."""
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

TARGETS = [
    'docs/safety.md',
    'docs/building.md',
    'docs/blood-tests.md',
    'docs/ftm-hrt.md',
    'docs/localization.md',
    'docs/pk-differences.md',
    'docs/release-process.md',
    'docs/reminders.md',
    'docs/third-party-notices.md',
    'docs/widget.md',
    'docs/backup-format.md',
    'docs/architecture.md',
    'docs/data-model.md',
]

for rel in TARGETS:
    p = os.path.join(ROOT, rel)
    if not os.path.exists(p):
        print('SKIP missing:', rel)
        continue
    with open(p, 'r', encoding='utf-8') as f:
        content = f.read()
    before = content.count('Featherline')
    new = content.replace('Featherline', 'PicoHRT')
    after = new.count('Featherline')
    if new != content:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(new)
        print(f'UPDATED: {rel} (replaced {before - after}, remaining {after})')
    else:
        print(f'NO CHANGE: {rel} (count={before})')
