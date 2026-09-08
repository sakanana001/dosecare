"""把 v0.4.json 写入 assets/ 和 test resources/, 不带 BOM."""
import json
from pathlib import Path

ROOT = Path(r"C:\Users\yuwen\Desktop\精品神药")
src = ROOT / "data" / "drugs" / "v0.4.json"
targets = [
    ROOT / "app" / "src" / "main" / "assets" / "drugs" / "v0.4.json",
    ROOT / "app" / "src" / "test" / "resources" / "drugs" / "v0.4.json",
]

# 读源(去掉 BOM)
raw = src.read_bytes()
if raw[:3] == b'\xef\xbb\xbf':
    raw = raw[3:]
text = raw.decode("utf-8")

# 验证 JSON OK
data = json.loads(text)
print(f"source: {len(data['drugs'])} drugs, schema {data.get('schemaVersion')}")

for t in targets:
    # 用 utf-8 编码但 'utf-8' 而不是 'utf-8-sig' → 不写 BOM
    t.write_bytes(text.encode("utf-8"))
    # 验证读回没 BOM
    rb = t.read_bytes()
    has_bom = rb[:3] == b'\xef\xbb\xbf'
    print(f"wrote {t.name}: {len(rb)} bytes, BOM={has_bom}")
