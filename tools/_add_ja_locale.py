"""第 3 期: 给底座加第 5 语言(日语)。

现状:
- 底座 4 语言:values(英) / values-b+zh+Hans / values-b+zh+Hant / values-b+yue+Hant+HK,共 1035 key
- DoseCare 有 values-ja(548 key),但只覆盖它自己的 key
- 合并后的 key 大半来自 PicoHRT(widget/日记/库存/计划/设置…),DoseCare 的 ja 里没有

本次只做 A 类: DoseCare ja 有、且底座也需要的 key —— 也就是本次合并引入的
药理目录 + 相互作用检测那批(144 个)。B 类 891 个(底座独有)没有日文译文,
留着后续补 —— Android 的 fallback 会回落到默认英文,不会崩,只是界面上
日语会夹杂英文。

用法: python tools/_add_ja_locale.py [--apply]
"""

import io
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
WS = os.path.dirname(ROOT)
DST = os.path.join(ROOT, 'app/src/main/res')
SRC = os.path.join(WS, 'dosecare/app/src/main/res')

APPLY = '--apply' in sys.argv


def load(path):
    if not os.path.exists(path):
        return {}
    c = io.open(path, encoding='utf-8').read()
    out = {}
    for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', c, re.S):
        out[m.group(1)] = m.group(2)
    return out


def main():
    base_en = load(os.path.join(DST, 'values/strings.xml'))
    dc_ja = load(os.path.join(SRC, 'values-ja/strings.xml'))

    # A 类: DoseCare ja 有,且底座默认(en)也有 -> 说明底座真的在用它
    carry = {k: v for k, v in dc_ja.items() if k in base_en}

    print('底座 en key: %d' % len(base_en))
    print('DoseCare ja key: %d' % len(dc_ja))
    print('可直接搬运(A 类): %d' % len(carry))
    print('无日文译文(B 类,留待后续): %d' % (len(base_en) - len(carry)))
    print()

    target_dir = os.path.join(DST, 'values-ja')
    target = os.path.join(target_dir, 'strings.xml')

    if os.path.exists(target):
        existing = load(target)
        missing = {k: v for k, v in carry.items() if k not in existing}
        print('values-ja 已存在, 有 %d 条, 需补 %d 条' % (len(existing), len(missing)))
        carry = missing
        raw = open(target, 'rb').read()
        text = raw.decode('utf-8-sig')
    else:
        text = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<resources>\n'
            '</resources>\n'
        )
        print('values-ja 新建,写入 %d 条' % len(carry))

    if not carry:
        print('[dry-run] 无需写入')
        return

    # 跟随底座 strings.xml 的行尾(是 LF)
    newline = '\r\n' if '\r\n' in text else '\n'
    lines = [
        '    <!-- v0.9g merge: 来自 DoseCare values-ja 的药理目录 / 相互作用检测译文 -->',
    ]
    for key in sorted(carry):
        lines.append('    <string name="%s">%s</string>' % (key, carry[key]))
    block = newline.join(lines) + newline

    text = text.replace('</resources>', block + '</resources>')
    os.makedirs(target_dir, exist_ok=True)
    with io.open(target, 'w', encoding='utf-8', newline='') as f:
        f.write(text)
    print('  PATCH values-ja/strings.xml  +%d 条' % len(carry))

    # 按前缀给个分布,便于判断覆盖
    import collections
    pref = collections.Counter(k.split('_')[0] for k in carry)
    print()
    print('本次日语覆盖的前缀:')
    for k, v in pref.most_common():
        print('    %-14s %d' % (k, v))


if __name__ == '__main__':
    main()
