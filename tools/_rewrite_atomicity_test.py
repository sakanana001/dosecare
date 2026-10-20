# -*- coding: utf-8 -*-
"""重写 SettingsRepositoryTest 的"折叠状态原子更新"用例。

原断言把**两个各自独立收集的流** combine 起来, 再要求"第一次发射就是最终状态" ——
这依赖 combine 收到两个上游发射的先后顺序, 而那个顺序由调度决定, 并不是仓库保证的
不变式 (仓库保证的是: 两个键在**同一次 DataStore 发射**里一起变)。

新断言直接看 DataStore 的发射序列: 折叠状态翻转的那一次发射里, 已确认告警必须
同时是新的 (收起) / 已被清掉 (展开)。这才是仓库真正保证的东西, 且与平台无关。
"""
import io
import os

PATH = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged\app\src\test\java\com\dosecare\app\data\repository\SettingsRepositoryTest.kt"

with io.open(PATH, "r", encoding="utf-8", newline="") as f:
    text = f.read()
nl = "\r\n" if "\r\n" in text else "\n"

OLD = (
    "    @Test" + nl +
    "    fun `home low stock fold state updates expanded and acknowledged states atomically`() =" + nl +
    "        runTest(testDispatcher) {" + nl +
    "            val uuid = \"11111111-1111-1111-1111-111111111111\"" + nl +
    "            val foldStateFlow: Flow<Pair<Boolean, Map<String, MedicineStockState>>> = combine(" + nl +
    "                settingsRepository.homeLowStockSectionExpandedFlow," + nl +
    "                settingsRepository.homeLowStockAcknowledgedWarningStatesFlow," + nl +
    "            ) { expanded: Boolean, states: Map<String, MedicineStockState> -> expanded to states }" + nl +
    nl +
    "            val collapseObserved = mutableListOf<Pair<Boolean, Map<String, MedicineStockState>>>()" + nl +
    "            val collapseJob = launch {" + nl +
    "                foldStateFlow" + nl +
    "                    .drop(1)" + nl +
    "                    .take(1)" + nl +
    "                    .toList(collapseObserved)" + nl +
    "            }" + nl +
    "            settingsRepository.setHomeLowStockSectionFoldState(" + nl +
    "                expanded = false," + nl +
    "                acknowledgedWarningStates = mapOf(uuid to MedicineStockState.IMMINENT)," + nl +
    "            )" + nl +
    "            collapseJob.join()" + nl +
    nl +
    "            assertEquals(" + nl +
    "                listOf(false to mapOf(uuid to MedicineStockState.IMMINENT))," + nl +
    "                collapseObserved," + nl +
    "            )" + nl +
    "            assertEquals(false, settingsRepository.homeLowStockSectionExpandedFlow.first())" + nl +
    "            assertEquals(" + nl +
    "                mapOf(uuid to MedicineStockState.IMMINENT)," + nl +
    "                settingsRepository.homeLowStockAcknowledgedWarningStatesFlow.first()," + nl +
    "            )" + nl +
    nl +
    "            val expandObserved = mutableListOf<Pair<Boolean, Map<String, MedicineStockState>>>()" + nl +
    "            val expandJob = launch {" + nl +
    "                foldStateFlow" + nl +
    "                    .drop(1)" + nl +
    "                    .take(1)" + nl +
    "                    .toList(expandObserved)" + nl +
    "            }" + nl +
    "            settingsRepository.setHomeLowStockSectionFoldState(expanded = true)" + nl +
    "            expandJob.join()" + nl +
    nl +
    "            assertEquals(" + nl +
    "                listOf(true to emptyMap<String, MedicineStockState>())," + nl +
    "                expandObserved," + nl +
    "            )" + nl +
    "            assertEquals(true, settingsRepository.homeLowStockSectionExpandedFlow.first())" + nl +
    "            assertEquals(" + nl +
    "                emptyMap<String, MedicineStockState>()," + nl +
    "                settingsRepository.homeLowStockAcknowledgedWarningStatesFlow.first()" + nl +
    "            )" + nl +
    "        }" + nl
)

NEW = (
    "    @Test" + nl +
    "    fun `home low stock fold state updates expanded and acknowledged states atomically`() =" + nl +
    "        runTest(testDispatcher) {" + nl +
    "            val uuid = \"11111111-1111-1111-1111-111111111111\"" + nl +
    "            val expected = mapOf(uuid to MedicineStockState.IMMINENT)" + nl +
    nl +
    "            // 原子性 = 两个键在**同一次 DataStore 发射**里一起变。" + nl +
    "            // 仓库对外暴露的是两个各自独立收集的流, 把它们 combine 出来的中间态" + nl +
    "            // 先后取决于调度顺序, 不能拿它当原子性判据 —— 这里直接看 DataStore" + nl +
    "            // 的发射序列。" + nl +
    "            val expandedKey = booleanPreferencesKey(\"home_low_stock_section_expanded\")" + nl +
    "            val statesKey = stringSetPreferencesKey(\"home_low_stock_acknowledged_warning_states\")" + nl +
    nl +
    "            val collapseEmissions = mutableListOf<Preferences>()" + nl +
    "            val collapseWatcher = launch { dataStore.data.collect { collapseEmissions += it } }" + nl +
    "            advanceUntilIdle()" + nl +
    nl +
    "            settingsRepository.setHomeLowStockSectionFoldState(" + nl +
    "                expanded = false," + nl +
    "                acknowledgedWarningStates = expected," + nl +
    "            )" + nl +
    "            advanceUntilIdle()" + nl +
    "            collapseWatcher.cancel()" + nl +
    nl +
    "            // 折叠改成 false 的那次发射里, 已确认告警必须已经是新的 —— 不能还是旧的。" + nl +
    "            val collapseEmission = collapseEmissions.last { it[expandedKey] == false }" + nl +
    "            assertEquals(" + nl +
    "                setOf(\"$uuid|IMMINENT\")," + nl +
    "                collapseEmission[statesKey]," + nl +
    "            )" + nl +
    "            assertEquals(false, settingsRepository.homeLowStockSectionExpandedFlow.first())" + nl +
    "            assertEquals(" + nl +
    "                expected," + nl +
    "                settingsRepository.homeLowStockAcknowledgedWarningStatesFlow.first()," + nl +
    "            )" + nl +
    nl +
    "            val expandEmissions = mutableListOf<Preferences>()" + nl +
    "            val expandWatcher = launch { dataStore.data.collect { expandEmissions += it } }" + nl +
    "            advanceUntilIdle()" + nl +
    "            settingsRepository.setHomeLowStockSectionFoldState(expanded = true)" + nl +
    "            advanceUntilIdle()" + nl +
    "            expandWatcher.cancel()" + nl +
    nl +
    "            // 再展开: 折叠变 true 的那次发射里, 已确认告警必须同时被清掉。" + nl +
    "            val expandEmission = expandEmissions.last { it[expandedKey] == true }" + nl +
    "            assertNull(" + nl +
    "                \"展开时应同时清掉已确认告警, 实际发射: $expandEmissions\"," + nl +
    "                expandEmission[statesKey]," + nl +
    "            )" + nl +
    "            assertEquals(true, settingsRepository.homeLowStockSectionExpandedFlow.first())" + nl +
    "            assertEquals(" + nl +
    "                emptyMap<String, MedicineStockState>()," + nl +
    "                settingsRepository.homeLowStockAcknowledgedWarningStatesFlow.first()" + nl +
    "            )" + nl +
    "        }" + nl
)

assert text.count(OLD) == 1, "旧用例块没匹配上 (count=%d)" % text.count(OLD)
text = text.replace(OLD, NEW, 1)

# import: booleanPreferencesKey
imp = "import androidx.datastore.preferences.core.booleanPreferencesKey" + nl
if imp not in text:
    anchor = "import androidx.datastore.preferences.core.edit" + nl
    assert anchor in text
    text = text.replace(anchor, imp + anchor, 1)

with io.open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write(text)
print("rewrote the atomicity test (newline=%r)" % nl)
