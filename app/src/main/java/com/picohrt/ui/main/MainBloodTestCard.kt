package com.picohrt.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.picohrt.model.bloodtest.BloodAnalyteReferenceSeverity
import com.picohrt.model.bloodtest.BloodUnitKey
import com.picohrt.util.formatCalibrationConvertedValue
import com.picohrt.util.formatCalibrationUnitLabel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * v2 血检追踪 (BT-PR2): the home-screen "Recent Bloodwork" card.
 *
 * Renders the user's most recent T blood-test trend (spark line over
 * the last 6 months with a per-goal reference band overlay) and the
 * most recent hemoglobin value (per-goal severity chip). The card
 * is a no-op when [cardState] is null (i.e. the user has no blood
 * test panels yet); the parent surface skips the call in that case.
 *
 * Layout (vertical):
 * - Header: "Recent Bloodwork" + sample-count subtitle
 * - T trend row: trend title + spark line + latest value
 * - Hb chip row: chip with value + severity dot + collected-at date
 *
 * The card is intentionally narrow (no scrolling, no tabs) — it
 * surfaces a one-glance "where am I in my regimen" snapshot. Users
 * who want to drill into the full panel list go to the calibration
 * screen via a future "View all" affordance (BT-PR3 scope).
 */
@Composable
internal fun MainBloodTestCard(
    cardState: MainBloodTestCardUiState,
    zoneId: ZoneId,
    appLocale: Locale,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("MainBloodTestCard"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MainBloodTestCardHeader(
                panelCount = cardState.totalPanelCount,
            )
            val testosteroneTrend = cardState.testosteroneTrend
            if (testosteroneTrend != null) {
                MainBloodTestCardTTrendRow(
                    trend = testosteroneTrend,
                    profile = cardState.testosteroneProfile,
                    appLocale = appLocale,
                )
            } else {
                MainBloodTestCardEmptyRow(
                    text = appLocale.bloodTestCardNoTestosteroneHint(),
                )
            }
            val hemoglobinChip = cardState.hemoglobinChip
            if (hemoglobinChip != null) {
                MainBloodTestCardHbChipRow(
                    chip = hemoglobinChip,
                    zoneId = zoneId,
                )
            } else {
                MainBloodTestCardEmptyRow(
                    text = appLocale.bloodTestCardNoHemoglobinHint(),
                )
            }
        }
    }
}

@Composable
private fun MainBloodTestCardHeader(panelCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Recent Bloodwork",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val subtitle = if (panelCount == 1) {
            "1 panel"
        } else {
            "$panelCount panels"
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MainBloodTestCardTTrendRow(
    trend: MainBloodTrendUiState,
    profile: com.picohrt.model.bloodtest.BloodAnalyteReferenceProfile?,
    appLocale: Locale,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Testosterone",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatBloodTrendValue(
                    canonicalValue = trend.latestValueCanonical,
                    canonicalUnit = BloodUnitKey.NG_DL,
                    locale = appLocale,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            MainBloodTestCardSparkLine(
                trend = trend,
                profile = profile,
                modifier = Modifier.fillMaxWidth().height(40.dp),
            )
        }
    }
}

@Composable
private fun MainBloodTestCardSparkLine(
    trend: MainBloodTrendUiState,
    profile: com.picohrt.model.bloodtest.BloodAnalyteReferenceProfile?,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (trend.points.isEmpty()) return@Canvas
        val yRange = (trend.maxY - trend.minY).coerceAtLeast(1e-9)

        // Reference band: if a profile is provided and its
        // normalRange fits inside the chart, shade the
        // corresponding Y region. If the band is wider than the
        // chart we skip the shading to avoid drawing a
        // chart-filling rectangle that hides the line.
        if (profile != null) {
            val bandLow = profile.normalRange.start
            val bandHigh = profile.normalRange.endInclusive
            val bandLowY = height - ((bandLow - trend.minY) / yRange * height).toFloat()
            val bandHighY = height - ((bandHigh - trend.minY) / yRange * height).toFloat()
            val bandTop = bandLowY.coerceAtMost(0f)
            val bandBottom = bandHighY.coerceAtLeast(height)
            val visibleTop = bandTop.coerceIn(0f, height)
            val visibleBottom = bandBottom.coerceIn(0f, height)
            if (visibleBottom > visibleTop) {
                drawRect(
                    color = bandColor,
                    topLeft = Offset(0f, visibleTop),
                    size = Size(width, visibleBottom - visibleTop),
                )
            }
        }

        // Build the line path. X is mapped from offsetDays
        // (negative for past) to [0, width]; the latest point is
        // at offsetDays == 0 (right edge) and the oldest is at
        // offsetDays == -windowDays (left edge).
        val path = Path()
        val sortedPoints = trend.points.sortedBy { it.offsetDays }
        sortedPoints.forEachIndexed { index, point ->
            val xFraction = ((point.offsetDays + trend.windowDays).toFloat() / trend.windowDays.toFloat())
                .coerceIn(0f, 1f)
            val x = xFraction * width
            val y = height - ((point.valueCanonical - trend.minY) / yRange * height).toFloat()
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx()),
        )

        // Subtle baseline at the bottom of the chart so the
        // spark line visually anchors even with a single data
        // point.
        drawLine(
            color = gridColor,
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 1f,
        )
    }
}

@Composable
private fun MainBloodTestCardHbChipRow(
    chip: MainHemoglobinChipUiState,
    zoneId: ZoneId,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Hemoglobin",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        MainBloodTestCardHbChip(chip = chip, zoneId = zoneId)
    }
}

@Composable
private fun MainBloodTestCardHbChip(
    chip: MainHemoglobinChipUiState,
    zoneId: ZoneId,
) {
    val container = chip.severity.containerColor()
    val content = chip.severity.contentColor()
    val date = chip.collectedAt.atZone(zoneId).toLocalDate()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = content,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(chip.severity.dotColor()),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatBloodTrendValue(
                    canonicalValue = chip.valueCanonical,
                    canonicalUnit = com.picohrt.model.bloodtest.BloodTestCatalog.canonicalUnitFor(
                        com.picohrt.model.bloodtest.BloodAnalyteKey.HEMOGLOBIN
                    ),
                    locale = Locale.getDefault(),
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "·",
                style = MaterialTheme.typography.labelLarge,
                color = content.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = chip.collectedAt.atZone(zoneId).toLocalDate()
                    .format(shortDateFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.8f),
            )
        }
    }
}

private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
private fun MainBloodTestCardEmptyRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Format a canonical blood test value (e.g. ng/dL for T, g/dL for Hb)
 * into a one-decimal display string with the unit label. Uses the
 * existing blood test formatters so the card stays consistent with
 * the calibration editor's value display.
 */
private fun formatBloodTrendValue(
    canonicalValue: Double,
    canonicalUnit: BloodUnitKey,
    locale: Locale,
): String {
    val numeric = formatCalibrationConvertedValue(canonicalValue)
    val unitLabel = formatCalibrationUnitLabel(canonicalUnit.storageValue)
    return "$numeric $unitLabel"
}

private fun Locale.bloodTestCardNoTestosteroneHint(): String =
    if (this == Locale.SIMPLIFIED_CHINESE || this == Locale.CHINESE || this == Locale.TRADITIONAL_CHINESE) {
        "尚无睾酮血检数据"
    } else {
        "No testosterone blood tests yet"
    }

private fun Locale.bloodTestCardNoHemoglobinHint(): String =
    if (this == Locale.SIMPLIFIED_CHINESE || this == Locale.CHINESE || this == Locale.TRADITIONAL_CHINESE) {
        "尚无血红蛋白血检数据"
    } else {
        "No hemoglobin blood tests yet"
    }

@Composable
private fun BloodAnalyteReferenceSeverity.containerColor(): Color = when (this) {
    BloodAnalyteReferenceSeverity.NORMAL,
    BloodAnalyteReferenceSeverity.UNKNOWN,
    -> MaterialTheme.colorScheme.surfaceContainerHighest
    BloodAnalyteReferenceSeverity.LOW -> MaterialTheme.colorScheme.tertiaryContainer
    BloodAnalyteReferenceSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
    BloodAnalyteReferenceSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun BloodAnalyteReferenceSeverity.contentColor(): Color = when (this) {
    BloodAnalyteReferenceSeverity.NORMAL,
    BloodAnalyteReferenceSeverity.UNKNOWN,
    -> MaterialTheme.colorScheme.onSurfaceVariant
    BloodAnalyteReferenceSeverity.LOW -> MaterialTheme.colorScheme.onTertiaryContainer
    BloodAnalyteReferenceSeverity.HIGH,
    BloodAnalyteReferenceSeverity.CRITICAL,
    -> MaterialTheme.colorScheme.onErrorContainer
}

@Composable
private fun BloodAnalyteReferenceSeverity.dotColor(): Color = when (this) {
    BloodAnalyteReferenceSeverity.NORMAL,
    BloodAnalyteReferenceSeverity.UNKNOWN,
    -> MaterialTheme.colorScheme.onSurfaceVariant
    BloodAnalyteReferenceSeverity.LOW -> MaterialTheme.colorScheme.tertiary
    BloodAnalyteReferenceSeverity.HIGH -> MaterialTheme.colorScheme.error
    BloodAnalyteReferenceSeverity.CRITICAL -> MaterialTheme.colorScheme.error
}
