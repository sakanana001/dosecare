"""更彻底: 把 cyp subs/inh/ind 列表里所有 {"c": X, "f": Y} 改成 {"cyp": X, "fraction": Y}"""
import re
from pathlib import Path

# pattern: 在 cyp: { ... } 块内, 匹配每个 {"c": "X", "f": 1.0} 块
#  块内可能有 nested "cyp" key, 所以需要 bracket-balancing
# 简化: 匹配 {"c": "...", "f": 1.0} 和 {"c": "...", "f": 1.0, "note": "..."}
# 因为只是 c/f 字段在 subs/inh/ind dict 里出现, 直接正则替换

for fname in ['gen_v0.4_part1.py', 'gen_v0.4_part2.py']:
    p = Path(fname)
    text = p.read_text(encoding='utf-8')

    # pattern 1: {"c": "X", "f": 1.0}
    # pattern 2: {"c": "X", "f": 1.0, "note": "..."}
    # pattern 3: {"c": "X"} 单独
    # pattern 4: {"c": "X", "f": 1.0, "pop": "..."}
    # 直接匹配 "c": "..." -> "cyp": "..." 但只限 cyp sub dict (即前后是 { 和 , 或 })
    # 用更精确的 pattern: 匹配 {'c': "..", "f": 1.0} 或 {"c": "..", "f": 1.0}

    def fix_dict(m):
        # 整个 dict 字符串, 把 'c' / 'f' 替换
        s = m.group(0)
        s = re.sub(r'(\{|,)\s*"c"\s*:', r'\1"cyp":', s)
        s = re.sub(r"(\{|,)\s*'c'\s*:", r"\1'cyp':", s)
        s = re.sub(r'"f"\s*:', '"fraction":', s)
        s = re.sub(r"'f'\s*:", "'fraction':", s)
        return s

    # 匹配 {...} 但内部不含嵌套 {}。 即: 形如 { "c": ..., "f": ...[, "note": "..."] }
    # 用 [^{}]* 排除嵌套
    pattern = re.compile(r'\{[^{}]*?"c"[^{}]*?\}')
    matches = pattern.findall(text)
    print(f"{fname}: found {len(matches)} cyp-like dicts to fix")
    new_text = pattern.sub(fix_dict, text)
    if new_text != text:
        p.write_text(new_text, encoding='utf-8')
        print(f"[FIXED] {fname}")
    else:
        print(f"[unchanged] {fname}")
