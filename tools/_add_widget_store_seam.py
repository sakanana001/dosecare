# -*- coding: utf-8 -*-
"""给 WidgetAppearanceStore 加一个可注入的存储后端 (生产行为完全不变)。

背景: 单元测试没法用真的文件版 DataStore —— Windows 上只要还有活跃订阅者,
DataStore 就一直占着偏好文件, 它自己的原子改名会失败 (详见
app/src/test/java/com/dosecare/app/testing/InMemoryPreferencesDataStore.kt)。

生产路径仍然是 context 的进程级单例文件存储; 新增的 internal 构造只给测试用。
"""
import io
import os
import re

PATH = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged\app\src\main\java\com\dosecare\app\widget\WidgetAppearanceStore.kt"

with io.open(PATH, "r", encoding="utf-8", newline="") as f:
    text = f.read()
nl = "\r\n" if "\r\n" in text else "\n"

# 1) import DataStore
if "import androidx.datastore.core.DataStore" not in text:
    text = text.replace(
        "import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler" + nl,
        "import androidx.datastore.core.DataStore" + nl
        + "import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler" + nl,
        1,
    )

# 2) 构造器 + storage 属性
old_ctor = (
    "@Singleton" + nl
    + "class WidgetAppearanceStore @Inject constructor(" + nl
    + "    @param:ApplicationContext private val context: Context," + nl
    + ") {" + nl
)
assert text.count(old_ctor) == 1
new_ctor = old_ctor + (
    "    // 存储后端。生产用 context 的进程级单例文件存储; 单元测试可注入内存实现 ——" + nl
    + "    // Windows 上 DataStore 只要还有活跃订阅者就会一直占着偏好文件, 它自己的原子" + nl
    + "    // 改名会失败 (详见 InMemoryPreferencesDataStore 的注释)。" + nl
    + "    private var injectedStorage: DataStore<Preferences>? = null" + nl
    + nl
    + "    internal constructor(" + nl
    + "        context: Context," + nl
    + "        storage: DataStore<Preferences>," + nl
    + "    ) : this(context) {" + nl
    + "        injectedStorage = storage" + nl
    + "    }" + nl
    + nl
    + "    private val storage: DataStore<Preferences>" + nl
    + "        get() = injectedStorage ?: context.widgetAppearanceDataStore" + nl
    + nl
)
text = text.replace(old_ctor, new_ctor, 1)

# 3) 所有 context.widgetAppearanceDataStore -> storage
before = text.count("context.widgetAppearanceDataStore")
# 只替换类体里的 (构造器里那处 "context.widgetAppearanceDataStore" 要保留)
text = text.replace("context.widgetAppearanceDataStore.data", "storage.data")
text = text.replace("context.widgetAppearanceDataStore.edit", "storage.edit")
after = text.count("context.widgetAppearanceDataStore")
print("替换 %d 处, 剩余 %d 处 (应只剩 storage getter 里那处)" % (before - after, after))

with io.open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write(text)
print("patched WidgetAppearanceStore.kt (newline=%r)" % nl)
