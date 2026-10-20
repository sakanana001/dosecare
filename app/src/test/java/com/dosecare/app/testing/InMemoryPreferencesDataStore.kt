package com.dosecare.app.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 内存版 Preferences DataStore —— 给单元测试用。
 *
 * ## 为什么不用真的文件版
 *
 * DataStore 的写盘是 "写 .tmp -> 原子改名覆盖目标文件"。**Windows 上目标文件只要被
 * 任何句柄打开着, 这个改名就会失败**, DataStore 会抛:
 *
 * ```
 * java.io.IOException: Unable to rename x.preferences_pb.tmp to x.preferences_pb.
 * This likely means that there are multiple instances of DataStore for this file.
 * ```
 *
 * 而 DataStore 的 `data` 流**只要还有活跃订阅者, 就会一直占着偏好文件**。仓库层
 * (SettingsRepository / WidgetAppearanceStore) 都有常驻收集器
 * (`stateIn(started = SharingStarted.Eagerly)` / 永不结束的 live flow), 于是:
 * 一次写 -> 收集器重新读文件 -> 文件句柄被占住 -> 下一次写必炸。
 *
 * Linux / Android 上 rename 覆盖一个已打开的文件是合法的, 所以 **App 本身完全没事**,
 * 只有在 Windows 上跑单元测试才会看到这批失败。降 datastore 版本也解决不了 (1.1.7
 * 与 1.2.1 表现一致)。
 *
 * ## 怎么实现的
 *
 * `Preferences` 在库里是抽象类, 外部构造不了 —— 所以这里**只持有 DataStore 库自己
 * 造出来的实例** (`edit {}` 内部的 `toPreferences()`), 不自己拼 Map:
 * `updateData` 把当前实例交给调用方的 transform, 再把 transform 返回的新实例存下来。
 *
 * ## 代价
 *
 * 这些单测测的是**仓库逻辑** (键的读写、迁移、默认值), 不是 DataStore 的 protobuf
 * 编解码; 换成内存实现后覆盖到的仍然是同一套逻辑。真机 / 仪器化测试仍然跑真的
 * 文件存储, 端到端路径不受影响。
 */
class InMemoryPreferencesDataStore : DataStore<Preferences> {

    private val mutex = Mutex()
    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = mutex.withLock {
        val updated = transform(state.value)
        state.value = updated
        updated
    }

    /** 当前内容的扁平快照 (键名 -> 值), 断言用。 */
    fun snapshot(): Map<String, Any> =
        state.value.asMap().entries.associate { (key, value) -> key.name to value }
}
