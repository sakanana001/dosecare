"""用 ASCII safe 方式测试 pinyin 字典。"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.path.insert(0, '.')
from pinyin_dict import to_pinyin_initials

cases = ['唑吡坦', '氯氮平', '氟伏沙明', '利培酮', '阿戈美拉汀', '丙戊酸钠', '锂']
for word in cases:
    result = to_pinyin_initials(word)
    print(f"word={word!r} (len={len(word)}) -> pinyin={result!r} (len={len(result)})")
    for i, c in enumerate(word):
        print(f"   {c!r} (U+{ord(c):04X}) -> {result[i]!r}")
