# -*- coding: utf-8 -*-
"""把受 DataStore Windows 文件锁影响的单元测试改用内存版 DataStore。

背景见 app/src/test/java/com/dosecare/app/testing/InMemoryPreferencesDataStore.kt 的注释。
本脚本做行级替换, 保留各文件原有行尾风格。
"""
import io
import os
import re

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
TEST = os.path.join(REPO, "app", "src", "test", "java", "com", "dosecare", "app")

TARGETS = [
    os.path.join("data", "repository", "SettingsRepositoryTest.kt"),
    os.path.join("data", "repository", "SettingsRepositoryLegacyWidgetAppearanceTest.kt"),
    os.path.join("ui", "catalog", "nudge", "StockNudgeGateTest.kt"),
]

# 匹配 `X = PreferenceDataStoreFactory.create(\n ...args... \n)` 整块
CREATE = re.compile(
    r"(?P<indent>[ \t]*)"
    r"(?P<name>dataStore|settingsDataStore|val dataStore: DataStore<Preferences>)"
    r"(?P<mid>\s*=\s*)"
    r"PreferenceDataStoreFactory\.create\(\s*"
    r"scope\s*=\s*\w+\s*,\s*"
    r"produceFile\s*=\s*\{[^{}]*\}\s*,?\s*"
    r"\)",
    re.S,
)


def read(path):
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        return f.read()


def write(path, text):
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)


for rel in TARGETS:
    path = os.path.join(TEST, rel)
    text = read(path)
    nl = "\r\n" if "\r\n" in text else "\n"
    orig = text

    def repl(m):
        indent = m.group("indent")
        name = m.group("name")
        if name.startswith("val "):
            return "%sval dataStore: DataStore<Preferences> = InMemoryPreferencesDataStore()" % indent
        return "%s%s= InMemoryPreferencesDataStore()" % (indent, name)

    text, n = CREATE.subn(repl, text)
    assert n == 1, "%s: create 块匹配 %d 次" % (rel, n)

    # 去掉不再需要的 import
    text = text.replace("import androidx.datastore.preferences.core.PreferenceDataStoreFactory" + nl, "")
    text = text.replace("import java.io.File" + nl, "")
    text = text.replace("import org.junit.rules.TemporaryFolder" + nl, "")

    # 去掉 TemporaryFolder 规则
    text = re.sub(
        r"[ \t]*@get:Rule" + re.escape(nl)
        + r"[ \t]*val tmpFolder: TemporaryFolder = TemporaryFolder\(\)" + re.escape(nl)
        + nl,
        "",
        text,
    )
    # Rule 的 import 在被删掉规则后可能没用了
    if "@get:Rule" not in text:
        text = text.replace("import org.junit.Rule" + nl, "")

    imp = "import com.dosecare.app.testing.InMemoryPreferencesDataStore" + nl
    if imp not in text:
        m = re.search(r"^import .+$", text, re.M)
        assert m, rel
        text = text[:m.start()] + imp + text[m.start():]

    if text != orig:
        write(path, text)
        print("patched %s (newline=%r)" % (rel, nl))
    else:
        print("unchanged %s" % rel)
