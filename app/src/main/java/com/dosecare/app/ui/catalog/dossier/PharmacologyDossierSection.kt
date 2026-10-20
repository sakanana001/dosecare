package com.dosecare.app.ui.catalog.dossier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dosecare.app.R
import com.dosecare.app.model.pharmacology.Drug
import com.dosecare.app.model.pharmacology.labelRes
import com.dosecare.app.ui.pharmacology.displayName
import com.dosecare.app.ui.components.HrtSection
import com.dosecare.app.ui.components.HrtSectionHeader
import com.dosecare.app.ui.components.PreferenceSegmentedListItem
import com.dosecare.app.ui.interaction.SearchableDrugPicker

/**
 * 药理档案区块 (v0.9g merge)。
 *
 * 把当前药品和 202 药理目录 (assets/pharmacology/catalog.json) 里的某一条 [Drug]
 * 关联起来。绑定值存在 medicines.drugCatalogId, 空串 = 未关联。
 *
 * 为什么放在详情页而不是新建流程:
 * - 新建药品时插一个目录选择器会打断主流程;
 * - 已存在的药也需要事后补绑定。
 * 详情页是"事后补全"的位置, 不打断任何已有流程。
 *
 * 搜索 UI 直接复用 ui/interaction/SearchableDrugPicker.kt: 202 药纯滚动找不到目标药,
 * 必须支持中文名 / 英文名 / 品牌名 / 拼音首字母实时过滤。
 */
@Composable
fun PharmacologyDossierSection(
    linkedDrug: Drug?,
    catalogDrugs: List<Drug>,
    onLink: (Drug) -> Unit,
    onUnlink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HrtSectionHeader(text = stringResource(R.string.dossier_section_title))
        SearchableDrugPicker(
            label = stringResource(R.string.dossier_picker_label),
            selected = linkedDrug,
            options = catalogDrugs,
            onSelect = onLink,
            placeholder = stringResource(R.string.dossier_search_placeholder),
            // 10% 色调底: 详情页区块已经很多, 药理档案是补充信息, 不该抢戏。
            chipColor = MaterialTheme.colorScheme.tertiary,
        )
        if (linkedDrug != null) {
            Spacer(Modifier.height(8.dp))
            HrtSection(title = null) {
                item {
                    DossierSummaryRow(drug = linkedDrug)
                }
                item {
                    DossierUnlinkRow(onClick = onUnlink)
                }
            }
        }
    }
}

/** 关联成功后的一行摘要: 中文名 + 分类 + 适应症。 */
@Composable
private fun DossierSummaryRow(drug: Drug) {
    val indicationLabels = drug.indicationGroups.map { group ->
        stringResource(group.labelRes())
    }
    val indicationText = if (indicationLabels.isEmpty()) {
        stringResource(R.string.dossier_no_indications)
    } else {
        indicationLabels.joinToString(" · ")
    }
    val categoryText = stringResource(drug.category.labelRes())
    PreferenceSegmentedListItem(
        title = drug.displayName(),
        supportingText = stringResource(
            R.string.dossier_summary_format,
            categoryText,
            indicationText,
        ),
        onClick = null,
        leadingContent = {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_labs),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/** 解除关联。动作可逆 (重新选一次即可), 不弹二次确认。 */
@Composable
private fun DossierUnlinkRow(onClick: () -> Unit) {
    PreferenceSegmentedListItem(
        title = stringResource(R.string.dossier_unlink_action),
        onClick = onClick,
        leadingContent = {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
