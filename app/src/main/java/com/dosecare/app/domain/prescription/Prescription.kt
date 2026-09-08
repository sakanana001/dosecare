package com.dosecare.app.domain.prescription

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 我的处方 - 数据模型 (v0.6 重构)
 *
 * 参考健康类 App "我的处方" 功能:
 * - 用户创建多个分组 (例如 "早上药", "睡前药", "急救药")
 * - 每个分组有自己的名称 + 纯色图标
 * - 每个分组下添加药物
 * - 打卡: 用户每次服药后记录 (时间 + 剂量 + 可选备注)
 * - 多次打卡可以**不同剂量** (e.g. 早 300mg + 晚 150mg)
 * - 曲线直接用打卡数据当 DoseEvent 跑 PkEngine
 *
 * 持久化: PrescriptionRepository 用 SharedPreferences 存 JSON
 */
@Serializable
data class PrescriptionGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorIndex: Int,                  // 0-7 → 8 种纯色
    val drugs: List<PrescribedDrug> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isEmpty(): Boolean = drugs.isEmpty()
    fun doseCount(): Int = drugs.sumOf { it.dosesTaken.size }
}

@Serializable
data class PrescribedDrug(
    val id: String = UUID.randomUUID().toString(),
    val drugId: String,                   // 关联 DrugCatalog.id
    val defaultDoseMg: Double,            // 默认/建议剂量 (添加药时填,打卡时也是初值)
    val frequencyPerDay: Int,             // 每日计划次数 (1-4), 仅显示
    val times: List<String> = emptyList(), // 自定义用药时间 "HH:mm", 为空时按 frequencyPerDay 默认
    val startDate: Long,                  // 起始日期 (epoch ms)
    val targetDate: Long? = null,         // 临时用药: 指定单次服药的日期 (epoch day 0:00 ms). null = 周期, set = 一次性
    val dosesTaken: List<DoseTaken> = emptyList()  // 历次打卡
) {
    fun lastDoseTime(): Long? = dosesTaken.maxOfOrNull { it.timestamp }

    fun todayDoseCount(): Int {
        val dayStart = todayStartMs()
        return dosesTaken.count { it.timestamp >= dayStart }
    }

    fun todayDoseSum(): Double = dosesTaken
        .filter { it.timestamp >= todayStartMs() }
        .sumOf { it.doseMg }

    private fun todayStartMs(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}

/**
 * 一次打卡事件
 *
 * 每条独立, 自由设定 timestamp + 剂量
 * - 用户"补卡"过去 (如忘记打卡, 半小时后想起来, 补登)
 * - 不同时间不同剂量 (e.g. 早 300mg + 晚 150mg)
 */
@Serializable
data class DoseTaken(
    val timestamp: Long,                  // 服药时间 (epoch ms)
    val doseMg: Double,                   // 本次剂量
    val note: String? = null              // 备注 (如 "早上起床后", "睡前")
)
