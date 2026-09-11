"""Bulk rebrand Featherline -> PicoHRT in selected files.

Run from repo root: python tools/_rebrand_strings.py
"""
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

# (relative path, [(old, new), ...])
JOBS = [
    ('app/src/main/res/values/strings.xml', [
        ('Featherline is a logging tool', 'PicoHRT is a logging tool'),
        ('To send reminders at your scheduled dose times, Featherline needs the following two permissions. You can revoke them at any time in your system settings.',
         'To send reminders at your scheduled dose times, PicoHRT needs the following two permissions. You can revoke them at any time in your system settings.'),
        ('To send reminders at your scheduled dose times, Featherline needs the following permission. You can revoke it at any time in your system settings.',
         'To send reminders at your scheduled dose times, PicoHRT needs the following permission. You can revoke it at any time in your system settings.'),
        ('Featherline can send reminders at your scheduled dose times. You can turn it off at any time in settings.',
         'PicoHRT can send reminders at your scheduled dose times. You can turn it off at any time in settings.'),
        ('Featherline can send reminders at your scheduled dose times. You can also allow exact alarms for more precise notification timing.',
         'PicoHRT can send reminders at your scheduled dose times. You can also allow exact alarms for more precise notification timing.'),
        ('These steps are optional, but can help Featherline provide more complete tracking, reminders, and estimates.',
         'These steps are optional, but can help PicoHRT provide more complete tracking, reminders, and estimates.'),
        ('>Open Featherline<', '>Open PicoHRT<'),
    ]),
    ('app/src/main/res/values-b+zh+Hans/strings.xml', [
        ('Featherline \u662f\u4e00\u4e2a\u8bb0\u5f55\u5de5\u5177\uff0c\u4e0d\u63d0\u4f9b\u533b\u7597\u5efa\u8bae\u3002',
         'PicoHRT \u662f\u4e00\u4e2a\u8bb0\u5f55\u5de5\u5177\uff0c\u4e0d\u63d0\u4f9b\u533b\u7597\u5efa\u8bae\u3002'),
        ('\u4e3a\u4e86\u5728\u60a8\u9884\u5b9a\u7684\u670d\u836f\u65f6\u95f4\u53d1\u9001\u63d0\u9192\uff0cFeatherline \u9700\u8981\u4ee5\u4e0b\u4e24\u9879\u6743\u9650\uff0c\u60a8\u53ef\u4ee5\u968f\u65f6\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5173\u95ed\u5b83\u4eec',
         '\u4e3a\u4e86\u5728\u60a8\u9884\u5b9a\u7684\u670d\u836f\u65f6\u95f4\u53d1\u9001\u63d0\u9192\uff0cPicoHRT \u9700\u8981\u4ee5\u4e0b\u4e24\u9879\u6743\u9650\uff0c\u60a8\u53ef\u4ee5\u968f\u65f6\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5173\u95ed\u5b83\u4eec'),
        ('\u4e3a\u4e86\u5728\u60a8\u9884\u5b9a\u7684\u670d\u836f\u65f6\u95f4\u53d1\u9001\u63d0\u9192\uff0cFeatherline \u9700\u8981\u4ee5\u4e0b\u6743\u9650\uff0c\u60a8\u53ef\u4ee5\u968f\u65f6\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5173\u95ed\u5b83',
         '\u4e3a\u4e86\u5728\u60a8\u9884\u5b9a\u7684\u670d\u836f\u65f6\u95f4\u53d1\u9001\u63d0\u9192\uff0cPicoHRT \u9700\u8981\u4ee5\u4e0b\u6743\u9650\uff0c\u60a8\u53ef\u4ee5\u968f\u65f6\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5173\u95ed\u5b83'),
        ('Featherline \u53ef\u5728\u60a8\u9884\u5b9a\u7684\u670d\u836f\u65f6\u95f4\u53d1\u9001\u63d0\u9192\uff0c\u60a8\u53ef\u4ee5\u968f\u65f6\u5728\u8bbe\u7f6e\u4e2d\u5173\u95ed\u5b83',
         'PicoHRT \u53ef\u5728\u60a8\u9884\u5b9a\u7684\u670d\u836f\u65f6\u95f4\u53d1\u9001\u63d0\u9192\uff0c\u60a8\u53ef\u4ee5\u968f\u65f6\u5728\u8bbe\u7f6e\u4e2d\u5173\u95ed\u5b83'),
        ('Featherline \u53ef\u5728\u60a8\u9884\u5b9a\u7684\u670d\u836f\u65f6\u95f4\u53d1\u9001\u63d0\u9192\uff0c\u60a8\u8fd8\u53ef\u4ee5\u5f00\u542f\u7cbe\u786e\u95f9\u9496\u4ee5\u4f7f\u901a\u77e5\u66f4\u51c6\u65f6',
         'PicoHRT \u53ef\u5728\u60a8\u9884\u5b9a\u7684\u670d\u836f\u65f6\u95f4\u53d1\u9001\u63d0\u9192\uff0c\u60a8\u8fd8\u53ef\u4ee5\u5f00\u542f\u7cbe\u786e\u95f9\u9496\u4ee5\u4f7f\u901a\u77e5\u66f4\u51c6\u65f6'),
        ('\u4ee5\u4e0b\u6b65\u9aa4\u4e3a\u53ef\u9009\u9879\uff0c\u53ef\u5e2e\u52a9 Featherline \u63d0\u4f9b\u66f4\u5b8c\u6574\u7684\u8bb0\u5f55\u3001\u63d0\u9192\u548c\u4f30\u7b97',
         '\u4ee5\u4e0b\u6b65\u9aa4\u4e3a\u53ef\u9009\u9879\uff0c\u53ef\u5e2e\u52a9 PicoHRT \u63d0\u4f9b\u66f4\u5b8c\u6574\u7684\u8bb0\u5f55\u3001\u63d0\u9192\u548c\u4f30\u7b97'),
        ('>\u6253\u5f00 Featherline<', '>\u6253\u5f00 PicoHRT<'),
    ]),
    ('app/src/main/res/values-b+zh+Hant/strings.xml', [
        ('app_name">Featherline<', 'app_name">PicoHRT<'),
        ('Featherline \u662f\u4e00\u500b\u8a18\u9304\u5de5\u5177\uff0c\u4e0d\u63d0\u4f9b\u91ab\u7642\u5efa\u8b70\u3002',
         'PicoHRT \u662f\u4e00\u500b\u8a18\u9304\u5de5\u5177\uff0c\u4e0d\u63d0\u4f9b\u91ab\u7642\u5efa\u8b70\u3002'),
        ('\u70ba\u4e86\u5728\u60a8\u9810\u5b9a\u7684\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0cFeatherline \u9700\u8981\u4ee5\u4e0b\u5169\u9805\u6b0a\u9650\uff0c\u60a8\u53ef\u4ee5\u96a8\u6642\u5728\u7cfb\u7d71\u8a2d\u5b9a\u4e2d\u95dc\u9589\u5b83\u5011',
         '\u70ba\u4e86\u5728\u60a8\u9810\u5b9a\u7684\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0cPicoHRT \u9700\u8981\u4ee5\u4e0b\u5169\u9805\u6b0a\u9650\uff0c\u60a8\u53ef\u4ee5\u96a8\u6642\u5728\u7cfb\u7d71\u8a2d\u5b9a\u4e2d\u95dc\u9589\u5b83\u5011'),
        ('\u70ba\u4e86\u5728\u60a8\u9810\u5b9a\u7684\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0cFeatherline \u9700\u8981\u4ee5\u4e0b\u6b0a\u9650\uff0c\u60a8\u53ef\u4ee5\u96a8\u6642\u5728\u7cfb\u7d71\u8a2d\u5b9a\u4e2d\u95dc\u9589\u5b83',
         '\u70ba\u4e86\u5728\u60a8\u9810\u5b9a\u7684\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0cPicoHRT \u9700\u8981\u4ee5\u4e0b\u6b0a\u9650\uff0c\u60a8\u53ef\u4ee5\u96a8\u6642\u5728\u7cfb\u7d71\u8a2d\u5b9a\u4e2d\u95dc\u9589\u5b83'),
        ('Featherline \u53ef\u5728\u60a8\u9810\u5b9a\u7684\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0c\u60a8\u53ef\u4ee5\u96a8\u6642\u5728\u8a2d\u5b9a\u4e2d\u95dc\u9589\u5b83',
         'PicoHRT \u53ef\u5728\u60a8\u9810\u5b9a\u7684\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0c\u60a8\u53ef\u4ee5\u96a8\u6642\u5728\u8a2d\u5b9a\u4e2d\u95dc\u9589\u5b83'),
        ('Featherline \u53ef\u5728\u60a8\u9810\u5b9a\u7684\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0c\u60a8\u9084\u53ef\u4ee5\u958b\u555f\u7cbe\u78ba\u95f9\u9418\u4ee5\u4f7f\u901a\u77e5\u66f4\u6e96\u6642',
         'PicoHRT \u53ef\u5728\u60a8\u9810\u5b9a\u7684\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0c\u60a8\u9084\u53ef\u4ee5\u958b\u555f\u7cbe\u78ba\u95f9\u9418\u4ee5\u4f7f\u901a\u77e5\u66f4\u6e96\u6642'),
        ('\u4ee5\u4e0b\u6b65\u9a5f\u70ba\u53ef\u9078\u9805\uff0c\u53ef\u5e6b\u52a9 Featherline \u63d0\u4f9b\u66f4\u5b8c\u6574\u7684\u8a18\u9304\u3001\u63d0\u9192\u548c\u4f30\u7b97',
         '\u4ee5\u4e0b\u6b65\u9a5f\u70ba\u53ef\u9078\u9805\uff0c\u53ef\u5e6b\u52a9 PicoHRT \u63d0\u4f9b\u66f4\u5b8c\u6574\u7684\u8a18\u9304\u3001\u63d0\u9192\u548c\u4f30\u7b97'),
        ('>\u958b\u555f Featherline<', '>\u958b\u555f PicoHRT<'),
    ]),
    ('app/src/main/res/values-b+yue+Hant+HK/strings.xml', [
        ('app_name">Featherline<', 'app_name">PicoHRT<'),
        ('Featherline \u4fcf\u4e00\u500b\u8a18\u9304\u5de5\u5177\uff0c\u5514\u63d0\u4f9b\u91ab\u7642\u5efa\u8b70\u3002',
         'PicoHRT \u4fcf\u4e00\u500b\u8a18\u9304\u5de5\u5177\uff0c\u5514\u63d0\u4f9b\u91ab\u7642\u5efa\u8b70\u3002'),
        ('\u70ba\u55ae\u568c\u4f60\u9810\u5b9a\u5605\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0cFeatherline \u9700\u8981\u4ee5\u4e0b\u5169\u9805\u6b0a\u9650\uff1b\u4f60\u53ef\u4ee5\u96a8\u6642\u568c\u7cfb\u7d71\u8a2d\u5b9a\u95dc\u9589\u4f96\u568c',
         '\u70ba\u55ae\u568c\u4f60\u9810\u5b9a\u5605\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0cPicoHRT \u9700\u8981\u4ee5\u4e0b\u5169\u9805\u6b0a\u9650\uff1b\u4f60\u53ef\u4ee5\u96a8\u6642\u568c\u7cfb\u7d71\u8a2d\u5b9a\u95dc\u9589\u4f96\u568c'),
        ('\u70ba\u55ae\u568c\u4f60\u9810\u5b9a\u5605\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0cFeatherline \u9700\u8981\u4ee5\u4e0b\u6b0a\u9650\uff1b\u4f60\u53ef\u4ee5\u96a8\u6642\u568c\u7cfb\u7d71\u8a2d\u5b9a\u95dc\u9589\u4f96',
         '\u70ba\u55ae\u568c\u4f60\u9810\u5b9a\u5605\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0cPicoHRT \u9700\u8981\u4ee5\u4e0b\u6b0a\u9650\uff1b\u4f60\u53ef\u4ee5\u96a8\u6642\u568c\u7cfb\u7d71\u8a2d\u5b9a\u95dc\u9589\u4f96'),
        ('Featherline \u53ef\u4ee5\u568c\u4f60\u9810\u5b9a\u5605\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0c\u4f60\u53ef\u4ee5\u96a8\u6642\u568c\u8a2d\u5b9a\u95dc\u9589\u4f96',
         'PicoHRT \u53ef\u4ee5\u568c\u4f60\u9810\u5b9a\u5605\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0c\u4f60\u53ef\u4ee5\u96a8\u6642\u568c\u8a2d\u5b9a\u95dc\u9589\u4f96'),
        ('Featherline \u53ef\u4ee5\u568c\u4f60\u9810\u5b9a\u5605\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0c\u4f60\u4ea6\u53ef\u4ee5\u958b\u555f\u7cbe\u78ba\u95f9\u9418\uff0c\u4ee4\u901a\u77e5\u66f4\u6e96\u6642',
         'PicoHRT \u53ef\u4ee5\u568c\u4f60\u9810\u5b9a\u5605\u670d\u85e5\u6642\u9593\u50b3\u9001\u63d0\u9192\uff0c\u4f60\u4ea6\u53ef\u4ee5\u958b\u555f\u7cbe\u78ba\u95f9\u9418\uff0c\u4ee4\u901a\u77e5\u66f4\u6e96\u6642'),
        ('\u4ee5\u4e0b\u6b65\u9a5f\u70ba\u53ef\u9078\u9805\uff0c\u53ef\u5e6b\u52a9 Featherline \u63d0\u4f9b\u66f4\u5b8c\u6574\u5605\u8a18\u9304\u3001\u63d0\u9192\u548c\u4f30\u7b97',
         '\u4ee5\u4e0b\u6b65\u9a5f\u70ba\u53ef\u9078\u9805\uff0c\u53ef\u5e6b\u52a9 PicoHRT \u63d0\u4f9b\u66f4\u5b8c\u6574\u5605\u8a18\u9304\u3001\u63d0\u9192\u548c\u4f30\u7b97'),
        ('>\u958b\u555f Featherline<', '>\u958b\u555f PicoHRT<'),
    ]),
    ('app/src/debug/res/values/strings.xml', [
        ('Featherline Debug', 'PicoHRT Debug'),
    ]),
    ('app/src/debug/res/values-b+zh+Hans/strings.xml', [
        ('Featherline Debug', 'PicoHRT Debug'),
    ]),
    ('fastlane/metadata/android/en-US/title.txt', [
        ('Featherline: HRT Tracker', 'PicoHRT: HRT Tracker'),
    ]),
    ('fastlane/metadata/android/en-US/full_description.txt', [
        ('Featherline is a thoughtful', 'PicoHRT is a thoughtful'),
        ('Featherline shows you how', 'PicoHRT shows you how'),
        ('About Featherline', 'About PicoHRT'),
        ('Featherline is a personal tracking tool', 'PicoHRT is a personal tracking tool'),
    ]),
    ('fastlane/metadata/android/zh-CN/title.txt', [
        ('Featherline: HRT \u8ddf\u8e2a', 'PicoHRT: HRT \u8ddf\u8e2a'),
    ]),
    ('fastlane/metadata/android/zh-CN/full_description.txt', [
        ('Featherline \u662f\u4e00\u6b3e', 'PicoHRT \u662f\u4e00\u6b3e'),
        ('Featherline \u628a\u4f60\u7684\u7528\u836f\u65f6\u95f4', 'PicoHRT \u628a\u4f60\u7684\u7528\u836f\u65f6\u95f4'),
        ('\u5173\u4e8e Featherline', '\u5173\u4e8e PicoHRT'),
        ('Featherline \u662f\u4e00\u6b3e\u4e2a\u4eba\u8bb0\u5f55\u5de5\u5177', 'PicoHRT \u662f\u4e00\u6b3e\u4e2a\u4eba\u8bb0\u5f55\u5de5\u5177'),
    ]),
    ('app/src/main/res/xml/hrt_widget_anchor_info.xml', [
        ('android:configure="com.mkx.hrttracker.widget.WidgetConfigActivity"',
         'android:configure="com.picohrt.widget.WidgetConfigActivity"'),
    ]),
    ('app/src/main/res/xml-v31/hrt_widget_large_info.xml', [
        ('android:configure="com.mkx.hrttracker.widget.WidgetConfigActivity"',
         'android:configure="com.picohrt.widget.WidgetConfigActivity"'),
    ]),
    ('app/src/main/res/xml-v31/hrt_widget_medium_info.xml', [
        ('android:configure="com.mkx.hrttracker.widget.WidgetConfigActivity"',
         'android:configure="com.picohrt.widget.WidgetConfigActivity"'),
    ]),
    ('benchmark/build.gradle.kts', [
        ('namespace = "com.mkx.hrttracker.macrobenchmark"',
         'namespace = "com.picohrt.macrobenchmark"'),
    ]),
    ('benchmark/src/main/java/com/mkx/hrttracker/macrobenchmark/ColdStartupBenchmark.kt', [
        ('package com.mkx.hrttracker.macrobenchmark',
         'package com.picohrt.macrobenchmark'),
        ('com.mkx.hrttracker.benchmark', 'com.picohrt.benchmark'),
    ]),
]


def main():
    failures = 0
    for rel, replacements in JOBS:
        p = os.path.join(ROOT, rel)
        if not os.path.exists(p):
            print('SKIP missing:', rel)
            failures += 1
            continue
        with open(p, 'r', encoding='utf-8') as f:
            content = f.read()
        orig = content
        per_file_missing = []
        for old, new in replacements:
            if old not in content:
                per_file_missing.append(old[:80])
            else:
                content = content.replace(old, new)
        if per_file_missing:
            for m in per_file_missing:
                print(f'  NOT FOUND in {rel}: {m}')
                failures += 1
        if content != orig:
            with open(p, 'w', encoding='utf-8') as f:
                f.write(content)
            print('UPDATED:', rel)
        else:
            print('NO CHANGE:', rel)
    if failures:
        print(f'failures={failures}')
        sys.exit(1)


if __name__ == '__main__':
    main()
