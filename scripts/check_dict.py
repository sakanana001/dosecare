import sys, importlib.util
spec = importlib.util.spec_from_file_location('pd', 'pinyin_dict.py')
pd = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pd)
for c in ['咖', '唳', '硝', '胚', '胍', '灵']:
    missing = pd.PINYIN_INITIAL.get(c, "MISSING")
    print(c + " (U+%04X) -> %r" % (ord(c), missing))
