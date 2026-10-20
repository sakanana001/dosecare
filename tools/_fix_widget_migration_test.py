# -*- coding: utf-8 -*-
"""WidgetAppearanceRepositoryMigrationTest 改用内存版存储。

顺带去掉 "反射拿进程级单例 delegate + 共享临时目录" 那套 —— 有了可注入的存储后端,
测试可以直接用内存实现, 不用再和进程级单例较劲。
"""
import io
import os
import re

PATH = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged\app\src\test\java\com\dosecare\app\widget\WidgetAppearanceRepositoryMigrationTest.kt"

with io.open(PATH, "r", encoding="utf-8", newline="") as f:
    text = f.read()
nl = "\r\n" if "\r\n" in text else "\n"

# 1) 去掉 TemporaryFolder 规则
text = re.sub(
    r"[ \t]*@get:Rule" + re.escape(nl)
    + r"[ \t]*val tmpFolder: TemporaryFolder = TemporaryFolder\(\)" + re.escape(nl)
    + nl,
    "",
    text,
)
text = text.replace("import org.junit.rules.TemporaryFolder" + nl, "")
if "@get:Rule" not in text:
    text = text.replace("import org.junit.Rule" + nl, "")

# 2) 去掉文件版 DataStore 的 import 与 Files/Context 之外的杂项
text = text.replace("import androidx.datastore.preferences.core.PreferenceDataStoreFactory" + nl, "")

# 3) setUp: 内存存储 + 注入
old_setup = (
    "        settingsDataStore = PreferenceDataStoreFactory.create(" + nl
    + "            scope = testScope," + nl
    + "            produceFile = { File(tmpFolder.root, \"migration_settings_test.preferences_pb\") }," + nl
    + "        )" + nl
    + "        settingsRepository = SettingsRepository(context, settingsDataStore)" + nl
    + nl
    + "        store = WidgetAppearanceStore(widgetContext)" + nl
    + "        repository = WidgetAppearanceRepository(store, settingsRepository, mockk(relaxed = true))" + nl
    + nl
    + "        // The widget DataStore singleton survives across tests; start each empty." + nl
    + "        runBlocking { widgetDataStore().edit { it.clear() } }" + nl
)
assert text.count(old_setup) == 1, "setUp 块没匹配上"
new_setup = (
    "        settingsDataStore = InMemoryPreferencesDataStore()" + nl
    + "        settingsRepository = SettingsRepository(context, settingsDataStore)" + nl
    + nl
    + "        // 内存版存储: Windows 上文件版 DataStore 会被常驻读句柄锁住, 写不进去。" + nl
    + "        widgetStorage = InMemoryPreferencesDataStore()" + nl
    + "        store = WidgetAppearanceStore(widgetContext, widgetStorage)" + nl
    + "        repository = WidgetAppearanceRepository(store, settingsRepository, mockk(relaxed = true))" + nl
)
text = text.replace(old_setup, new_setup, 1)

# 4) 字段声明
old_fields = (
    "    private lateinit var settingsDataStore: DataStore<Preferences>" + nl
    + "    private lateinit var settingsRepository: SettingsRepository" + nl
)
assert text.count(old_fields) == 1
new_fields = (
    "    private lateinit var settingsDataStore: DataStore<Preferences>" + nl
    + "    private lateinit var widgetStorage: DataStore<Preferences>" + nl
    + "    private lateinit var settingsRepository: SettingsRepository" + nl
)
text = text.replace(old_fields, new_fields, 1)

# 5) 干掉反射拿 delegate 的那套, 直接用注入的存储
old_helper = (
    "    // The store's DataStore is created by a process-wide singleton delegate keyed to" + nl
    + "    // the first Context's filesDir, so every test must share one stable directory" + nl
    + "    // (a per-test TemporaryFolder would leave the cached singleton pointing at a" + nl
    + "    // deleted path) and reset contents in setUp instead." + nl
    + "    private fun widgetDataStore(): DataStore<Preferences> {" + nl
    + "        val getter = Class.forName(\"com.dosecare.app.widget.WidgetAppearanceStoreKt\")" + nl
    + "            .getDeclaredMethod(\"getWidgetAppearanceDataStore\", Context::class.java)" + nl
    + "            .apply { isAccessible = true }" + nl
    + "        @Suppress(\"UNCHECKED_CAST\")" + nl
    + "        return getter.invoke(null, widgetContext) as DataStore<Preferences>" + nl
    + "    }" + nl
    + nl
)
assert text.count(old_helper) == 1, "helper 块没匹配上"
text = text.replace(old_helper, "", 1)

# 6) companion: 临时目录不再需要
old_companion = (
    "    private companion object {" + nl
    + "        val widgetFilesDir = Files.createTempDirectory(\"widget_appearance_test\").toFile()" + nl
    + "        val widgetContext: Context = mockk<Context>().also {" + nl
    + "            every { it.applicationContext } returns it" + nl
    + "            every { it.filesDir } returns widgetFilesDir" + nl
    + "        }" + nl
    + "    }" + nl
)
assert text.count(old_companion) == 1, "companion 块没匹配上"
new_companion = (
    "    private companion object {" + nl
    + "        val widgetContext: Context = mockk<Context>().also {" + nl
    + "            every { it.applicationContext } returns it" + nl
    + "        }" + nl
    + "    }" + nl
)
text = text.replace(old_companion, new_companion, 1)

# 7) import 整理
text = text.replace("import java.nio.file.Files" + nl, "")
if "import java.io.File" in text and "File(" not in text.split("import java.io.File")[1]:
    text = text.replace("import java.io.File" + nl, "")
if "runBlocking" not in text.replace("import kotlinx.coroutines.runBlocking" + nl, ""):
    text = text.replace("import kotlinx.coroutines.runBlocking" + nl, "")

imp = "import com.dosecare.app.testing.InMemoryPreferencesDataStore" + nl
if imp not in text:
    m = re.search(r"^import com\.dosecare\.app\..+$", text, re.M)
    assert m
    # 放到 com.dosecare.app.* 组里字母序正确的位置
    imports = [(mm.start(), mm.group(0)) for mm in re.finditer(r"^import .+$", text, re.M)]
    target = None
    for pos, line in imports:
        if line > imp:
            target = pos
            break
    if target is None:
        last = imports[-1]
        target = last[0] + len(last[1])
        text = text[:target] + nl + imp + text[target:]
    else:
        text = text[:target] + imp + nl + text[target:]

with io.open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write(text)
print("patched WidgetAppearanceRepositoryMigrationTest.kt (newline=%r)" % nl)
