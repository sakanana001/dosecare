"""从 v0.4.json 提取所有 unique Chinese chars, 排序输出, 给用户手动补拼音。"""
import json
import re
from pathlib import Path

ROOT = Path(r"C:\Users\yuwen\Desktop\精品神药")
data = json.load(open(ROOT / "data" / "drugs" / "v0.4.json", encoding="utf-8"))

# 收集所有 genericNameZh + brandNames + sub + category 字段里出现的中文字
all_chars = set()
for d in data["drugs"]:
    for field in ["genericNameZh"]:
        v = d.get(field, "")
        all_chars.update(c for c in v if '\u4e00' <= c <= '\u9fff')
    for b in d.get("brandNames", []):
        all_chars.update(c for c in b if '\u4e00' <= c <= '\u9fff')
    for s in [d.get("sub", ""), d.get("category", "")]:
        all_chars.update(c for c in s if '\u4e00' <= c <= '\u9fff')

# 排序
chars_sorted = sorted(all_chars)
print(f"Total unique Chinese chars: {len(chars_sorted)}")
# 输出到文件
out = ROOT / "scripts" / "chinese_chars.txt"
out.write_text("".join(chars_sorted), encoding="utf-8")
print(f"Wrote to {out}")
print("First 100 chars:", "".join(chars_sorted[:100]))
print("Last 50 chars:", "".join(chars_sorted[-50:]))
