package com.dosecare.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dosecare.app.R
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.DrugCategory

/**
 * 共享药卡 / 分类 chip / 治疗窗 / CYP 提示 / 免责声明 composables
 *
 * v0.3.2 改 4 底栏后,HomeScreen 主页取消(由 BottomNavTabs.TdmTab/CatalogTab 替代),
 * 这些 helper 仍被 CatalogTab / DrugDetailScreen 引用,所以保留并改为 internal。
 *
 * v0.9d i18n: 药物主名按当前 locale 显示 (zh 用 genericNameZh, en/ja 用 genericName),
 * 副名 (次要语言) 用 drug_name_with_*_subtitle format 包装。
 */
@Composable
internal fun DrugCard(drug: Drug, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val isChinesePrimary = locale.language == "zh"
    val primaryName = if (isChinesePrimary) drug.genericNameZh else drug.genericName
    val secondaryName = if (isChinesePrimary) drug.genericName else drug.genericNameZh
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(primaryName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        secondaryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CategoryChip(drug.category)
            }
            Spacer(Modifier.height(12.dp))
            WindowInfo(drug)
            Spacer(Modifier.height(8.dp))
            CypInfo(drug)
        }
    }
}

@Composable
internal fun CategoryChip(category: DrugCategory) {
    val color = categoryColor(category)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            category.displayName,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * 给每个 DrugCategory 一个稳定的颜色（基于 enum name 哈希到色相）
 * 避免 30+ when 分支维护地狱,加新 enum 自动有颜色
 */
internal fun categoryColor(category: DrugCategory): Color {
    // 已知 4 类用品牌色(眼睛已经适应)
    return when (category) {
        DrugCategory.ANTIPSYCHOTIC -> Color(0xFF7E5CAD)
        DrugCategory.MOOD_STABILIZER -> Color(0xFF2C5F8D)
        DrugCategory.ANTIDEPRESSANT -> Color(0xFF2E7D5B)
        DrugCategory.ANXIOLYTIC -> Color(0xFFB8763D)
        else -> hashToColor(category.name)
    }
}

private fun hashToColor(name: String): Color {
    // 稳定 hash -> 色相 [0, 360), 固定饱和/亮度保证可读
    val h = name.fold(0) { acc, c -> acc * 31 + c.code } and 0x7FFFFFFF
    val hue = (h % 360).toFloat()
    return Color.hsl(hue, saturation = 0.55f, lightness = 0.42f)
}

@Composable
internal fun WindowInfo(drug: Drug) {
    val window = drug.therapeuticWindow
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        if (window != null) {
            // TODO(v0.9b): "治疗窗:X - Y Z" 格式字符串需要 i18n (home_therapeutic_window 当前只接 1 个 %1$s)
            //            需要新增 key (如 home_window_range: 治疗窗:%1$s - %2$s %3$s)
            Text(
                stringResource(R.string.home_therapeutic_window, "${"%.0f".format(window.low)} - ${"%.0f".format(window.high)} ${window.unit}"),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                stringResource(R.string.home_no_window),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun CypInfo(drug: Drug) {
    val subs = drug.cypProfile.substrates.joinToString { "${it.cyp.displayName} (${(it.fraction * 100).toInt()}%)" }
    if (subs.isEmpty()) return
    Text(
        stringResource(R.string.home_metabolism, subs),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
internal fun DisclaimerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // TODO(v0.9b): 完整免责声明 (个体差异显著,实际用药请遵医嘱) 需要新增 strings.xml key
        //            home_disclaimer 仅有 "⚠️ 本 APP 所有计算结果(PK 估算、警示)仅供参考。"
        //            建议: home_disclaimer_full: "%1$s\n个体差异显著,实际用药请遵医嘱。"
        Text(
            stringResource(R.string.home_disclaimer),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
