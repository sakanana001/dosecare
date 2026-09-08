"""把 pinyin_dict.py 的 PINYIN_INITIAL dict 转成 Kotlin 字符串。
然后嵌入 SearchableDrugPicker.kt。"""
import sys
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location("pd", Path(__file__).parent / "pinyin_dict.py")
pd = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pd)

# 排序输出
lines = []
for c, p in sorted(pd.PINYIN_INITIAL.items(), key=lambda kv: ord(kv[0])):
    if not ('\u4e00' <= c <= '\u9fff') and not (c in '0123456789'):
        continue
    if c in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789':
        continue
    # 用 unicode 转义 (双引号 → String, 不是 Char)
    lines.append('    "\\u%04x" to "%s",' % (ord(c), p))

body = "\n".join(lines)
output = """// 自动从 scripts/pinyin_dict.py 生成
internal val PINYIN_INITIAL: Map<String, String> = mapOf(
""" + body + """
)

/** 中文 → 拼音首字母 (例: "唑吡坦" -> "zbt") */
internal fun toPinyinInitials(chinese: String): String =
    chinese.map { c -> PINYIN_INITIAL[c.toString()] ?: c.toString() }.joinToString("")
"""
out_path = Path(__file__).parent.parent / "app" / "src" / "main" / "java" / "com" / "dosecare" / "app" / "ui" / "PinyinInitial.kt"
out_path.write_text(output, encoding="utf-8")
print(f"Wrote {out_path} ({len(lines)} chars)")
