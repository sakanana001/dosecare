package com.dosecare.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dosecare.app.R
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.pk.PkCurve
import kotlin.math.abs
import kotlin.math.ceil

/**
 * 可交互浓度曲线 (v0.7 股票 K 线风格)
 *
 * 手势 (合并为单一 pipeline):
 * - **单击/轻触**: 显示 (t, c) 十字线 + 暗色 tooltip
 * - **单指拖动**: 左右平移 + tooltip 实时跟随手指 (在每条曲线交点处画实心圆, 圆色 = 该 drug.color)
 * - **双指捏合**: X 轴缩放 (1x ~ 5x)
 *
 * 视觉:
 * - 曲线粗度: 单药 7f, 多药 5f (避免重叠混乱)
 * - 交点圆: drug.color 填充 + 白色描边 (从曲线中被高亮出)
 * - 治疗窗色带 + 边界虚线
 * - 预测给药点 (实心 + 白边) vs 实际打卡 (橙色小点)
 *
 * Bug 修复:
 * - 修舍曲林等 cMax << window.high 的药: yMax 用 max(cMax*1.6, window.high*1.15, cMin*2, 1)
 *   (避免治疗窗覆盖整个图导致曲线看不见)
 */
data class ChartSeries(
    val drug: Drug,
    val curve: PkCurve,
    val color: Color,
    val plannedDoseTimes: List<Double> = emptyList(),
    val actualDoseTimes: List<Double> = emptyList()
)

@Composable
fun InteractiveConcentrationChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 280.dp,
    title: String = stringResource(R.string.chart_title),
    showTapTooltip: Boolean = true
) {
    if (series.isEmpty()) return
    val colorScheme = MaterialTheme.colorScheme

    // 状态: 平移偏移 + 缩放
    var offsetX by remember { mutableStateOf(0f) }
    var scaleX by remember { mutableStateOf(1f) }
    // 交互点 (单击或拖动手指位置)
    var interactionPoint by remember { mutableStateOf<Offset?>(null) }
    var interactionTime by remember { mutableStateOf(0.0) }
    // 每条曲线在 interactionTime 时刻的浓度 (用于交点圆 + tooltip)
    var interactionValues by remember { mutableStateOf(List(series.size) { 0.0 }) }

    // 固定原始 tMin/tMax (平移缩放前)
    val tMin = series.minOf { it.curve.t.firstOrNull() ?: 0.0 }
    val tMax = series.maxOf { it.curve.t.lastOrNull() ?: 24.0 }
    val tSpan = (tMax - tMin).coerceAtLeast(0.001)

    // 智能 yMax: 修复"舍曲林 bug" + 单位 bug
    // 所有数值在 yMax 计算前统一换算到 therapeuticWindow 同单位 (cMax × cMaxUnitFactor)
    // skipTherapeuticWindowBand=true 的药 (INR/ng/dL) 不参与 win 比较
    val yMax = series.maxOf { s ->
        val cmaxD = s.curve.cMax * s.drug.cMaxUnitFactor
        val cminD = s.curve.cMin * s.drug.cMaxUnitFactor
        val winHigh = if (s.drug.skipTherapeuticWindowBand) 0.0
                      else s.drug.therapeuticWindow?.high ?: 0.0
        maxOf(cmaxD * 1.6, winHigh * 1.15, cminD * 2.0, 1.0)
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
            )
            // TODO(v0.9d done): 改 R.string.chart_status_format
            Text(
                stringResource(R.string.chart_status_format, scaleX),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))

        // Legend
        series.forEach { s ->
            val factor = s.drug.cMaxUnitFactor
            val cmaxD = s.curve.cMax * factor  // 换算到治疗窗同单位
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(s.color, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${s.drug.genericNameZh} Cmax ${"%.1f".format(cmaxD)} ${s.drug.therapeuticWindow?.unit?.let { if (s.drug.skipTherapeuticWindowBand) it else it.substringBefore(" ") }}",
                    style = MaterialTheme.typography.labelSmall
                )
                s.drug.therapeuticWindow?.let { win ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.chart_window_label, win.low.toInt(), win.high.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = s.color
                    )
                    // TODO(v0.9d done): 4 个状态标签改 stringResource (chart_status_*)
                    val status = when {
                        s.drug.skipTherapeuticWindowBand -> stringResource(R.string.chart_status_not_comparable)
                        cmaxD < win.low -> stringResource(R.string.chart_status_low)
                        cmaxD > win.high -> stringResource(R.string.chart_status_high)
                        else -> stringResource(R.string.chart_status_in_window)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        status, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            s.drug.skipTherapeuticWindowBand -> colorScheme.onSurfaceVariant
                            cmaxD < win.low -> Color(0xFFFB8C00)
                            cmaxD > win.high -> Color(0xFFD32F2F)
                            else -> Color(0xFF388E3C)
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // 画布 + 合并手势 pipeline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .pointerInput(Unit) {
                        // 单一手势循环: 一指 pan + tooltip 跟随, 双指 zoom
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            val firstId = firstDown.id
                            var lastPos = firstDown.position
                            // 第一下立即触发 tooltip
                            interactionPoint = firstDown.position
                            val viewW0 = size.width - 50f - 16f
                            val norm0 = ((firstDown.position.x - 50f - offsetX) / (viewW0 * scaleX))
                                .coerceIn(0f, 1f)
                            interactionTime = tMin + norm0 * tSpan
                            interactionValues = series.map { predictCAtInCurve(it.curve, interactionTime) }
                            firstDown.consume()

                            while (true) {
                                val event = awaitPointerEvent()
                                val activeChanges = event.changes.filter { it.pressed }
                                if (activeChanges.isEmpty()) break

                                if (activeChanges.size >= 2) {
                                    // 双指 pinch zoom
                                    val p1 = activeChanges[0]
                                    val p2 = activeChanges[1]
                                    val curDist = (p2.position - p1.position).getDistance()
                                    val prevDist = (p2.previousPosition - p1.previousPosition).getDistance()
                                    if (prevDist > 0.1f) {
                                        val zoomFactor = curDist / prevDist
                                        val newScale = (scaleX * zoomFactor).coerceIn(1f, 5f)
                                        if (abs(newScale - scaleX) > 0.001f) {
                                            scaleX = newScale
                                            val viewW = size.width - 50f - 16f
                                            val totalW = viewW * scaleX
                                            val minOffset = viewW - totalW
                                            offsetX = offsetX.coerceIn(minOffset, 0f)
                                        }
                                    }
                                    p1.consume()
                                    p2.consume()
                                } else {
                                    // 单指 pan + tooltip 跟随
                                    val change = activeChanges.firstOrNull { it.id == firstId } ?: break
                                    val newPos = change.position
                                    val dx = newPos.x - lastPos.x
                                    val tentativeOffset = offsetX + dx
                                    val viewW = size.width - 50f - 16f
                                    val totalW = viewW * scaleX
                                    val minOffset = viewW - totalW
                                    val clamped = tentativeOffset.coerceIn(minOffset, 0f)
                                    offsetX = clamped
                                    // lastPos 调整: 让实际位移 = clamped - oldOffset (避免边界回弹时 lastPos 跳)
                                    lastPos = Offset(newPos.x - (tentativeOffset - clamped), newPos.y)

                                    // 实时更新 tooltip
                                    val norm = ((newPos.x - 50f - offsetX) / (viewW * scaleX))
                                        .coerceIn(0f, 1f)
                                    interactionTime = tMin + norm * tSpan
                                    interactionValues = series.map { predictCAtInCurve(it.curve, interactionTime) }
                                    interactionPoint = newPos
                                    change.consume()
                                }
                            }
                        }
                    }
            ) {
                val padLeft = 50f; val padRight = 16f; val padTop = 16f; val padBottom = 28f
                val w = size.width - padLeft - padRight
                val h = size.height - padTop - padBottom

                fun xOf(t: Double): Float =
                    padLeft + offsetX + ((t - tMin) / tSpan * w * scaleX).toFloat()
                fun yOf(c: Double): Float =
                    padTop + (h - (c / yMax * h).toFloat())

                val gridColor = Color(0xFFCCCCCC)

                // 网格 (横 4 段)
                for (i in 0..4) {
                    val yVal = yMax * i / 4
                    val y = yOf(yVal)
                    drawLine(gridColor, Offset(padLeft, y), Offset(size.width - padRight, y), 1f)
                }
                // 网格 (竖向)
                val stepHours = when {
                    tSpan / scaleX <= 12 -> 2.0
                    tSpan / scaleX <= 24 -> 6.0
                    tSpan / scaleX <= 72 -> 12.0
                    tSpan / scaleX <= 168 -> 24.0
                    else -> 48.0
                }
                val firstTick = ceil(tMin / stepHours) * stepHours
                var tick = firstTick
                while (tick <= tMax) {
                    val x = xOf(tick)
                    if (x in padLeft..(size.width - padRight)) {
                        drawLine(gridColor, Offset(x, padTop), Offset(x, padTop + h), 1f)
                    }
                    tick += stepHours
                }

                // 曲线粗度: 单药更粗, 多药略细避免重叠混乱
                val lineWidth = if (series.size == 1) 7f else 5f

                series.forEachIndexed { idx, s ->
                    val factor = s.drug.cMaxUnitFactor
                    val win = s.drug.therapeuticWindow
                    // 治疗窗色带 (skip 时不画)
                    if (win != null && win.high > 0 && !s.drug.skipTherapeuticWindowBand) {
                        val yL = yOf(win.low.coerceAtMost(yMax))
                        val yH = yOf(win.high.coerceAtMost(yMax))
                        drawRect(
                            color = s.color.copy(alpha = 0.10f),
                            topLeft = Offset(padLeft, yH),
                            size = Size(w * scaleX, (yL - yH).coerceAtLeast(0f))
                        )
                        if (yL in padTop..(padTop + h)) {
                            drawLine(
                                s.color.copy(alpha = 0.4f),
                                Offset(padLeft, yL), Offset(size.width - padRight, yL), 1.5f
                            )
                        }
                        if (yH in padTop..(padTop + h)) {
                            drawLine(
                                s.color.copy(alpha = 0.4f),
                                Offset(padLeft, yH), Offset(size.width - padRight, yH), 1.5f
                            )
                        }
                    }
                    // 曲线 (c × factor 换算到治疗窗同单位)
                    val path = Path()
                    var started = false
                    for (i in s.curve.t.indices) {
                        val t = s.curve.t[i]
                        val x = xOf(t)
                        if (x < padLeft - 5f) {
                            started = false
                            continue
                        }
                        if (x > size.width - padRight + 5f) break
                        val y = yOf(s.curve.c[i] * factor)
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(path = path, color = s.color, style = Stroke(width = lineWidth))
                    // 预测给药点
                    s.plannedDoseTimes.forEach { doseT ->
                        val x = xOf(doseT)
                        if (x in padLeft..(size.width - padRight)) {
                            val c = predictCAtInCurve(s.curve, doseT) * factor
                            val y = yOf(c)
                            if (y in padTop..(padTop + h)) {
                                drawCircle(color = s.color, radius = 7f, center = Offset(x, y))
                                drawCircle(
                                    color = Color.White, radius = 7f,
                                    center = Offset(x, y), style = Stroke(width = 2f)
                                )
                                drawLine(
                                    s.color.copy(alpha = 0.3f),
                                    Offset(x, padTop + h), Offset(x, y), 1f
                                )
                            }
                        }
                    }
                    // 实际打卡点 (橙色)
                    s.actualDoseTimes.forEach { doseT ->
                        val x = xOf(doseT)
                        if (x in padLeft..(size.width - padRight)) {
                            val c = predictCAtInCurve(s.curve, doseT) * factor
                            val y = yOf(c)
                            if (y in padTop..(padTop + h)) {
                                drawCircle(color = Color(0xFFFFA000), radius = 5f, center = Offset(x, y))
                            }
                        }
                    }
                    // 交点圆: 拖动 / 点击时, 在 interactionTime 时刻画实心圆 (drug.color + 白边)
                    if (showTapTooltip && interactionPoint != null) {
                        val crossX = xOf(interactionTime)
                        if (crossX in padLeft..(size.width - padRight)) {
                            val c = interactionValues[idx] * factor
                            val crossY = yOf(c)
                            if (crossY in padTop..(padTop + h)) {
                                drawCircle(color = s.color, radius = 9f, center = Offset(crossX, crossY))
                                drawCircle(
                                    color = Color.White, radius = 9f,
                                    center = Offset(crossX, crossY), style = Stroke(width = 2.5f)
                                )
                            }
                        }
                    }
                }

                // Y / X 标签
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY; textSize = 22f; isAntiAlias = true
                    }
                    for (i in 0..4) {
                        val yVal = yMax * i / 4
                        val y = yOf(yVal)
                        canvas.nativeCanvas.drawText("%.0f".format(yVal), 4f, y + 8f, paint)
                    }
                    tick = firstTick
                    while (tick <= tMax) {
                        val x = xOf(tick)
                        if (x in padLeft..(size.width - padRight)) {
                            val label = if (tick < 48) "${tick.toInt()}h"
                                else if (tick < 24 * 7) "${(tick / 24).toInt()}d"
                                else "${(tick / 24 / 7).toInt()}w"
                            canvas.nativeCanvas.drawText(label, x - 10f, size.height - 6f, paint)
                        }
                        tick += stepHours
                    }
                }

                // Tap / 拖动十字线 + tooltip
                if (showTapTooltip && interactionPoint != null) {
                    val tx = interactionPoint!!.x
                    val ty = interactionPoint!!.y
                    if (tx in padLeft..(size.width - padRight) && ty in padTop..(padTop + h)) {
                        drawLine(
                            Color(0xFF1976D2),
                            Offset(tx, padTop), Offset(tx, padTop + h), 1.2f
                        )
                        drawLine(
                            Color(0xFF1976D2),
                            Offset(padLeft, ty), Offset(size.width - padRight, ty), 1.2f
                        )
                        // tooltip 框 (高度自适应: 多药时变高)
                        val boxW = 160f
                        val lineCount = 1 + series.size  // 时间行 + 每个药一行
                        val boxH = 24f + 20f * lineCount
                        val boxX = (tx + boxW + 10f).coerceAtMost(size.width - boxW - 4f)
                        val boxY = (ty - boxH - 10f).coerceAtLeast(padTop)
                        drawRect(
                            color = Color(0xFF263238).copy(alpha = 0.94f),
                            topLeft = Offset(boxX, boxY),
                            size = Size(boxW, boxH)
                        )
                        drawIntoCanvas { canvas ->
                            val tp = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE; textSize = 22f; isAntiAlias = true
                            }
                            val tapHourStr = if (interactionTime < 48) "${"%.1f".format(interactionTime)} h"
                                else if (interactionTime < 24 * 7) "${"%.1f".format(interactionTime / 24)} d"
                                else "${"%.1f".format(interactionTime / 24 / 7)} w"
                            canvas.nativeCanvas.drawText("t = $tapHourStr", boxX + 8f, boxY + 22f, tp)
                            series.forEachIndexed { idx, s ->
                                val c = interactionValues[idx] * s.drug.cMaxUnitFactor
                                val tpDrug = android.graphics.Paint().apply {
                                    color = android.graphics.Color.argb(
                                        255,
                                        (s.color.red * 255).toInt(),
                                        (s.color.green * 255).toInt(),
                                        (s.color.blue * 255).toInt()
                                    )
                                    textSize = 22f; isAntiAlias = true; isFakeBoldText = true
                                }
                                val name = s.drug.genericNameZh
                                val shortName = if (name.length > 4) name.substring(0, 4) + "…" else name
                                canvas.nativeCanvas.drawText(
                                    "$shortName c=${"%.1f".format(c)}",
                                    boxX + 8f, boxY + 24f + 20f * (idx + 1), tpDrug
                                )
                            }
                        }
                    }
                }
            }
        }

        // 重置按钮
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        scaleX = 1f; offsetX = 0f
                        interactionPoint = null
                    }
            ) {
                Text(
                    stringResource(R.string.chart_reset_view, "${"%.1f".format(scaleX)}x"),
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

private fun predictCAtInCurve(curve: PkCurve, t: Double): Double {
    if (curve.t.isEmpty()) return 0.0
    if (t <= curve.t.first()) return curve.c.first()
    if (t >= curve.t.last()) return curve.c.last()
    var lo = 0; var hi = curve.t.size - 1
    while (lo < hi - 1) {
        val mid = (lo + hi) / 2
        if (curve.t[mid] < t) lo = mid else hi = mid
    }
    val t0 = curve.t[lo]; val t1 = curve.t[hi]
    val c0 = curve.c[lo]; val c1 = curve.c[hi]
    val dt = t1 - t0
    return if (dt < 1e-9) c0 else c0 + (c1 - c0) * (t - t0) / dt
}
