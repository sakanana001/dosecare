"""修 part1/part2 里 cyp dicts: 'c' -> 'cyp', 'f' -> 'fraction'.
只影响 cyp subs/inh/ind 列表里的 dict, 其它 'f' 字段 (pk.f bioavailability) 单独处理.
"""
import re
from pathlib import Path

for fname in ['gen_v0.4_part1.py', 'gen_v0.4_part2.py']:
    p = Path(fname)
    text = p.read_text(encoding='utf-8')
    # 匹配 cyp subs/inh/ind 列表内的 'c' 和 'f' 字段:
    #  这些 dict 的格式: {"c": "X", "f": 1.0} 或 {'c': 'X', 'f': 1.0}
    #  它们的特征: 出现在 cyp: {...} 块内
    # 简单做法: 在 cyp: 块内做替换
    def fix_cyp_block(m):
        block = m.group(0)
        block = re.sub(r'"c":', '"cyp":', block)
        block = re.sub(r"'c':", "'cyp':", block)
        block = re.sub(r'"f":', '"fraction":', block)
        block = re.sub(r"'f':", "'fraction':", block)
        return block

    # 匹配 "cyp": {...} 整个块 (跨行, 非贪婪)
    new_text = re.sub(r'"cyp":\s*\{[^}]*\}', fix_cyp_block, text, flags=re.DOTALL)
    new_text = re.sub(r"'cyp':\s*\{[^}]*\}", fix_cyp_block, new_text, flags=re.DOTALL)

    if new_text != text:
        p.write_text(new_text, encoding='utf-8')
        print(f"[FIXED] {fname}")
    else:
        print(f"[unchanged] {fname}")
