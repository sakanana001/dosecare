"""把 v0.5.json 复制到 assets + test resources, 不带 BOM."""
import json
from pathlib import Path

ROOT = Path(r"C:\Users\yuwen\Desktop\精品神药")
src = ROOT / "data" / "drugs" / "v0.5.json"

# 读源 (去掉 BOM)
raw = src.read_bytes()
if raw[:3] == b'\xef\xbb\xbf':
    raw = raw[3:]
text = raw.decode("utf-8")
data = json.loads(text)
print(f"source: {len(data['drugs'])} drugs, schema {data.get('schemaVersion')}")

# 写两个目标 (无 BOM, 无 CRLF 转换)
targets = [
    ROOT / "app" / "src" / "main" / "assets" / "drugs" / "v0.5.json",
    ROOT / "app" / "src" / "test" / "resources" / "drugs" / "v0.5.json",
]
for t in targets:
    t.write_bytes(text.encode("utf-8"))
    rb = t.read_bytes()
    has_bom = rb[:3] == b'\xef\xbb\xbf'
    print(f"wrote {t.name}: {len(rb)} bytes, BOM={has_bom}")
