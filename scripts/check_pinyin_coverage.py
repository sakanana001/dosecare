import json
d = json.loads(open("app/src/main/assets/drugs/v0.6.json", encoding="utf-8").read())
# 收集所有中文字符
chs = set()
for drug in d["drugs"]:
    for c in drug["genericNameZh"]:
        if '\u4e00' <= c <= '\u9fff':
            chs.add(c)
print("catalog 中文字符数:", len(chs))

# 读 PINYIN_INITIAL (从 SearchableDrugPicker.kt)
src = open("app/src/main/java/com/dosecare/app/ui/SearchableDrugPicker.kt", encoding="utf-8").read()
import re
# 找 "\u4e00" to "y" 模式
pinyin_chars = set()
for m in re.finditer(r'"(\\u[0-9a-f]{4})" to "([a-z])"', src):
    char = m.group(1).encode().decode('unicode-escape')
    pinyin_chars.add(char)
print("PINYIN_INITIAL map 大小:", len(pinyin_chars))

missing = chs - pinyin_chars
print("catalog 中缺拼音的中文字符:", missing, " 数量:", len(missing))
print("未在 catalog 用到的拼音字符:", len(pinyin_chars - chs))
