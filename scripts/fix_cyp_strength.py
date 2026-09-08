"""把 inh/ind 列表里 dict 的 's' 字段改成 'strength'"""
import re
from pathlib import Path

for fname in ['gen_v0.4_part1.py', 'gen_v0.4_part2.py']:
    p = Path(fname)
    text = p.read_text(encoding='utf-8')
    # 匹配 {"cyp": "X", "s": "STRONG"} 形式 (subs 没 's', 只有 inh/ind 才有)
    # 这些是 cyp 块内嵌的 inh/ind 列表
    # pattern: {"cyp": "...", "s": "..."} - 在 cyp: {...} 块内
    pattern = re.compile(r'(\{"cyp":\s*"[^"]*",\s*)"s":')
    new_text = pattern.sub(r'\1"strength":', text)
    if new_text != text:
        p.write_text(new_text, encoding='utf-8')
        print(f"[FIXED] {fname}")
    else:
        print(f"[unchanged] {fname}")
