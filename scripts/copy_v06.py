"""复制 v0.6.json 到 assets + test resources, 不带 BOM."""
import json
from pathlib import Path

ROOT = Path(r"C:\Users\yuwen\Desktop\精品神药")
src = ROOT / "data" / "drugs" / "v0.6.json"
raw = src.read_bytes()
if raw[:3] == b'\xef\xbb\xbf':
    raw = raw[3:]
text = raw.decode("utf-8")
data = json.loads(text)
print(f"source: {len(data['drugs'])} drugs, schema {data.get('schemaVersion')}")

for t in [
    ROOT / "app" / "src" / "main" / "assets" / "drugs" / "v0.6.json",
    ROOT / "app" / "src" / "test" / "resources" / "drugs" / "v0.6.json",
]:
    t.write_bytes(text.encode("utf-8"))
    bom_marker = b'\xef\xbb\xbf'
    head = t.read_bytes()[:3]
    has_bom = head == bom_marker
    print("wrote %s: %d bytes, BOM=%s" % (t.name, t.stat().st_size, has_bom))
