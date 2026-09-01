package com.muandrew.forecast.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muandrew.forecast.model.Money
import kotlin.math.abs
import kotlin.math.max

enum class EntryChartMode {
    ASSET_POOL,      // Balance curve + superimposed input/contribution and withdrawal bars (Dual-Axis)
    INCOME_STREAM,   // Annual income bars only
    COMPOUNDING_DEBT,// Loan debt balance curve (inverse) + payment / interest bars (Dual-Axis)
    EXPENSE_STREAM,  // Annual expense outflow bars
    CASH_AVAILABLE   // Bidirectional bars (Green above $0 for surplus, Red below $0 for deficits/problems)
}

data class YearTrajectoryPoint(
    val calendarYear: Int,
    val age: Int,
    val balance: Money = Money.ZERO,
    val inflow: Money = Money.ZERO,     // Contributions / Income
    val outflow: Money = Money.ZERO,    // Withdrawals / Expenses / Debt Payments
    val isDeficit: Boolean = false,     // True if withdrawals exceed available balance in pool or cash deficit
    val shortfall: Money = Money.ZERO,  // Amount of unfunded withdrawal deficit
    val netCash: Money = Money.ZERO     // Signed cashflow (+ for surplus, - for deficit)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryTrajectoryChart(
    title: String,
    points: List<YearTrajectoryPoint>,
    chartMode: EntryChartMode,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var hoveredIndex by remember { mutableStateOf<Int?>(null) }

    val activeIndex = hoveredIndex ?: (points.size / 2)
    val activePoint = points.getOrNull(activeIndex) ?: points.first()
    val hasAnyDeficit = points.any { it.isDeficit || it.netCash.value < 0L }

    // Dual-Axis & Bidirectional Maximum Values:
    val maxBalanceCents = max(100L, points.maxOfOrNull { it.balance.value } ?: 100L)
    val maxInflowCents = points.maxOfOrNull { it.inflow.value } ?: 0L
    val maxOutflowCents = points.maxOfOrNull { it.outflow.value } ?: 0L
    val maxFlowCents = max(100L, max(maxInflowCents, maxOutflowCents))

    // Bidirectional Cash Available bounds:
    val maxPositiveCashCents = max(100L, points.filter { it.netCash.value > 0L }.maxOfOrNull { it.netCash.value } ?: 100L)
    val maxNegativeCashCents = points.filter { it.netCash.value < 0L }.maxOfOrNull { -it.netCash.value } ?: 0L
    val totalCashSpanCents = max(100L, maxPositiveCashCents + maxNegativeCashCents)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Top Header Row: Title on Left, Compact Active Inspection HUD on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Compact Active Inspection Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(Color(0xFF262626), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "Yr ${activePoint.calendarYear} (${activePoint.age}):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (chartMode == EntryChartMode.CASH_AVAILABLE) {
                    if (activePoint.netCash.value < 0L) {
                        Text(
                            "-${Money(-activePoint.netCash.value).toFormattedString()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF5350)
                        )
                    } else {
                        Text(
                            "+${activePoint.netCash.toFormattedString()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }
                }

                if (chartMode == EntryChartMode.ASSET_POOL || chartMode == EntryChartMode.COMPOUNDING_DEBT) {
                    if (activePoint.isDeficit) {
                        Text(
                            "Depleted (-${activePoint.shortfall.toFormattedString()})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF5350)
                        )
                    } else {
                        Text(
                            activePoint.balance.toFormattedString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }

                if (activePoint.inflow.value > 0L && chartMode != EntryChartMode.CASH_AVAILABLE) {
                    Text(
                        "+${activePoint.inflow.toFormattedString()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )
                }

                if (activePoint.outflow.value > 0L && chartMode != EntryChartMode.CASH_AVAILABLE) {
                    Text(
                        "-${activePoint.outflow.toFormattedString()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF5350)
                    )
                }
            }
        }

        // 2. Responsive Flexbox Legend (FlowRow) that wraps cleanly on mobile phone screens
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (chartMode) {
                EntryChartMode.CASH_AVAILABLE -> {
                    LegendBadge(label = "Surplus (+)", color = Color(0xFF81C784))
                    LegendBadge(label = "Deficit (-)", color = Color(0xFFEF5350))
                    LegendBadge(label = "\$0 Base", color = Color.White)
                }
                EntryChartMode.ASSET_POOL -> {
                    LegendBadge(label = "Balance (Max ${Money(maxBalanceCents).toFormattedString()})", color = accentColor)
                    LegendBadge(label = "Deposits (+)", color = Color(0xFF81C784))
                    LegendBadge(label = "Withdrawals (-)", color = Color(0xFFEF5350))
                    if (hasAnyDeficit) {
                        LegendBadge(label = "⚠️ Over-withdrawn", color = Color(0xFFEF5350))
                    }
                }
                EntryChartMode.INCOME_STREAM -> {
                    LegendBadge(label = "Pay (Max ${Money(maxFlowCents).toFormattedString()}/yr)", color = Color(0xFF81C784))
                }
                EntryChartMode.COMPOUNDING_DEBT -> {
                    LegendBadge(label = "Debt (Max ${Money(maxBalanceCents).toFormattedString()})", color = Color(0xFFEF5350))
                    LegendBadge(label = "Payments (-)", color = Color(0xFF64B5F6))
                }
                EntryChartMode.EXPENSE_STREAM -> {
                    LegendBadge(label = "Outflow (Max ${Money(maxFlowCents).toFormattedString()}/yr)", color = Color(0xFFEF5350))
                }
            }
        }

        // 3. Canvas Trajectory & Bars (with Dual-Axis, Bidirectional Cash Available & Deficit Shading)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        hoveredIndex = (ratio * (points.size - 1)).toInt()
                    }
                }
                .pointerInput(points) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        hoveredIndex = (ratio * (points.size - 1)).toInt()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padBottom = 16f
                val chartHeight = h - padBottom
                val count = points.size

                if (count < 2) return@Canvas

                val stepX = w / (count - 1).toFloat()
                val barWidth = max(3f, (stepX * 0.55f))

                // Special Bidirectional Mode: CASH_AVAILABLE
                if (chartMode == EntryChartMode.CASH_AVAILABLE) {
                    val yZero = if (maxNegativeCashCents == 0L) {
                        chartHeight
                    } else {
                        (maxPositiveCashCents.toFloat() / totalCashSpanCents.toFloat()) * chartHeight
                    }

                    // 0. Red Warning Background for Negative Deficit Years
                    points.forEachIndexed { i, pt ->
                        if (pt.netCash.value < 0L) {
                            val x = i * stepX
                            val startX = max(0f, x - stepX * 0.5f)
                            val endX = minOf(w, x + stepX * 0.5f)
                            val colWidth = endX - startX

                            drawRect(
                                color = Color(0xFFEF5350).copy(alpha = 0.28f),
                                topLeft = Offset(startX, 0f),
                                size = Size(colWidth, chartHeight)
                            )
                        }
                    }

                    // Draw Horizontal Zero Axis Line ($0.00 Base)
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = Offset(0f, yZero),
                        end = Offset(w, yZero),
                        strokeWidth = 1.5f
                    )

                    // Draw Bidirectional Cash Available Bars
                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val isHovered = (i == activeIndex)
                        val cash = pt.netCash.value

                        if (cash > 0L) {
                            val barH = (cash.toFloat() / totalCashSpanCents.toFloat()) * chartHeight
                            val barAlpha = if (hoveredIndex == null || isHovered) 0.88f else 0.45f

                            drawRect(
                                color = Color(0xFF81C784).copy(alpha = barAlpha),
                                topLeft = Offset(x - barWidth / 2f, yZero - barH),
                                size = Size(barWidth, barH)
                            )

                            if (isHovered) {
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(x - barWidth / 2f, yZero - barH),
                                    size = Size(barWidth, barH),
                                    style = Stroke(width = 1.5f)
                                )
                            }
                        } else if (cash < 0L) {
                            val absCash = -cash
                            val barH = (absCash.toFloat() / totalCashSpanCents.toFloat()) * chartHeight
                            val barAlpha = if (hoveredIndex == null || isHovered) 0.95f else 0.50f

                            drawRect(
                                color = Color(0xFFEF5350).copy(alpha = barAlpha),
                                topLeft = Offset(x - barWidth / 2f, yZero),
                                size = Size(barWidth, barH)
                            )

                            drawRect(
                                color = if (isHovered) Color.White else Color(0xFFFF5252),
                                topLeft = Offset(x - barWidth / 2f, yZero),
                                size = Size(barWidth, barH),
                                style = Stroke(width = if (isHovered) 2f else 1.2f)
                            )
                        }
                    }

                    // Active Hover Scrubber Line
                    if (hoveredIndex != null) {
                        val hoverX = activeIndex * stepX
                        val isDef = activePoint.netCash.value < 0L
                        drawLine(
                            color = if (isDef) Color(0xFFEF5350) else Color.White.copy(alpha = 0.7f),
                            start = Offset(hoverX, 0f),
                            end = Offset(hoverX, chartHeight),
                            strokeWidth = if (isDef) 2f else 1.5f
                        )
                    }
                    return@Canvas
                }

                // Standard Modes (Asset Pool, Income, Debt, Expense):
                // 0. Draw Red Background Shading for Over-withdrawn / Deficit Zones
                points.forEachIndexed { i, pt ->
                    if (pt.isDeficit || (pt.balance.value == 0L && pt.outflow.value > 0L)) {
                        val x = i * stepX
                        val startX = max(0f, x - stepX * 0.5f)
                        val endX = minOf(w, x + stepX * 0.5f)
                        val colWidth = endX - startX

                        // Full height red warning background tint
                        drawRect(
                            color = Color(0xFFEF5350).copy(alpha = 0.32f),
                            topLeft = Offset(startX, 0f),
                            size = Size(colWidth, chartHeight)
                        )

                        // Top warning red accent border
                        drawLine(
                            color = Color(0xFFEF5350).copy(alpha = 0.9f),
                            start = Offset(startX, 0f),
                            end = Offset(endX, 0f),
                            strokeWidth = 2.5f
                        )

                        // Bottom depleted baseline red line (so zero balance is clearly visible!)
                        drawLine(
                            color = Color(0xFFFF5252),
                            start = Offset(startX, chartHeight),
                            end = Offset(endX, chartHeight),
                            strokeWidth = 3.5f
                        )
                    }
                }

                // Draw background horizontal grid guides
                drawLine(
                    color = Color(0xFF2C2C2C),
                    start = Offset(0f, 0f),
                    end = Offset(w, 0f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF262626),
                    start = Offset(0f, chartHeight * 0.5f),
                    end = Offset(w, chartHeight * 0.5f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF383838),
                    start = Offset(0f, chartHeight),
                    end = Offset(w, chartHeight),
                    strokeWidth = 1f
                )

                // 1. Draw Superimposed Bars on the Secondary Bar Axis Scale (maxFlowCents)
                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val isHovered = (i == activeIndex)

                    // Inflow bar (Contribution / Income)
                    if (pt.inflow.value > 0L) {
                        val barH = (pt.inflow.value.toFloat() / maxFlowCents.toFloat()) * (chartHeight * 0.85f)
                        val barAlpha = if (hoveredIndex == null || isHovered) 0.85f else 0.40f

                        drawRect(
                            color = Color(0xFF81C784).copy(alpha = barAlpha),
                            topLeft = Offset(x - barWidth / 2f, chartHeight - barH),
                            size = Size(barWidth, barH)
                        )

                        if (isHovered) {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(x - barWidth / 2f, chartHeight - barH),
                                size = Size(barWidth, barH),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }

                    // Outflow bar (Withdrawal / Debt Payment / Expense)
                    if (pt.outflow.value > 0L) {
                        val barH = (pt.outflow.value.toFloat() / maxFlowCents.toFloat()) * (chartHeight * 0.85f)
                        val barColor = if (chartMode == EntryChartMode.COMPOUNDING_DEBT) Color(0xFF64B5F6) else Color(0xFFEF5350)
                        val barAlpha = if (hoveredIndex == null || isHovered) 0.90f else 0.45f

                        drawRect(
                            color = barColor.copy(alpha = barAlpha),
                            topLeft = Offset(x - barWidth / 2f, chartHeight - barH),
                            size = Size(barWidth, barH)
                        )

                        if (isHovered || pt.isDeficit) {
                            drawRect(
                                color = if (pt.isDeficit) Color(0xFFFF5252) else Color.White,
                                topLeft = Offset(x - barWidth / 2f, chartHeight - barH),
                                size = Size(barWidth, barH),
                                style = Stroke(width = if (pt.isDeficit) 2f else 1.5f)
                            )
                        }
                    }
                }

                // 2. Draw Total Asset Balance Curve (Segment-by-segment to turn RED upon depletion/over-withdrawal)
                if (chartMode == EntryChartMode.ASSET_POOL || chartMode == EntryChartMode.COMPOUNDING_DEBT) {
                    for (i in 0 until count - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val x1 = i * stepX
                        val y1 = chartHeight - ((p1.balance.value.toFloat() / maxBalanceCents.toFloat()) * chartHeight)
                        val x2 = (i + 1) * stepX
                        val y2 = chartHeight - ((p2.balance.value.toFloat() / maxBalanceCents.toFloat()) * chartHeight)

                        val isSegmentDeficit = p2.isDeficit || (p2.balance.value == 0L && p2.outflow.value > 0L)
                        val segmentColor = if (isSegmentDeficit) Color(0xFFEF5350) else accentColor

                        // Draw filled area quad under this segment
                        val segArea = Path().apply {
                            moveTo(x1, chartHeight)
                            lineTo(x1, y1)
                            lineTo(x2, y2)
                            lineTo(x2, chartHeight)
                            close()
                        }
                        drawPath(
                            path = segArea,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    segmentColor.copy(alpha = if (isSegmentDeficit) 0.40f else 0.25f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = chartHeight
                            )
                        )

                        // Draw segment line
                        drawLine(
                            color = segmentColor,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )

                        // If entering deficit, draw a warning beacon dot
                        if (isSegmentDeficit) {
                            drawCircle(
                                color = Color(0xFFEF5350),
                                radius = 4f,
                                center = Offset(x2, y2)
                            )
                        }
                    }
                }

                // 3. Draw Active Hover Scrubber Line and Indicator
                if (hoveredIndex != null) {
                    val hoverX = activeIndex * stepX
                    val activeIsDeficit = activePoint.isDeficit
                    drawLine(
                        color = if (activeIsDeficit) Color(0xFFEF5350) else Color.White.copy(alpha = 0.7f),
                        start = Offset(hoverX, 0f),
                        end = Offset(hoverX, chartHeight),
                        strokeWidth = if (activeIsDeficit) 2f else 1.5f
                    )

                    if (chartMode == EntryChartMode.ASSET_POOL || chartMode == EntryChartMode.COMPOUNDING_DEBT) {
                        val hoverY = chartHeight - ((activePoint.balance.value.toFloat() / maxBalanceCents.toFloat()) * chartHeight)
                        drawCircle(
                            color = if (activeIsDeficit) Color(0xFFEF5350) else Color.White,
                            radius = 5.5f,
                            center = Offset(hoverX, hoverY)
                        )
                        drawCircle(
                            color = if (activeIsDeficit) Color(0xFFFF5252) else accentColor,
                            radius = 3.5f,
                            center = Offset(hoverX, hoverY)
                        )
                    }
                }
            }

            // Direct Floating Numerical Badge over the Highlighted Bar/Point
            if (hoveredIndex != null) {
                val ratio = activeIndex.toFloat() / max(1f, (points.size - 1).toFloat())
                val alignment = when {
                    ratio < 0.25f -> Alignment.TopStart
                    ratio > 0.75f -> Alignment.TopEnd
                    else -> Alignment.TopCenter
                }

                val isDeficitActive = activePoint.isDeficit || activePoint.netCash.value < 0L

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF141414).copy(alpha = 0.94f), RoundedCornerShape(6.dp))
                            .border(
                                1.dp,
                                if (isDeficitActive) Color(0xFFEF5350) else Color(0xFF64B5F6),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Yr ${activePoint.calendarYear} (${activePoint.age})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (chartMode == EntryChartMode.CASH_AVAILABLE) {
                            if (activePoint.netCash.value < 0L) {
                                Text(
                                    "Deficit: -${Money(-activePoint.netCash.value).toFormattedString()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF5350)
                                )
                            } else {
                                Text(
                                    "Cash: +${activePoint.netCash.toFormattedString()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                            }
                        }

                        if (chartMode == EntryChartMode.ASSET_POOL || chartMode == EntryChartMode.COMPOUNDING_DEBT) {
                            if (activePoint.isDeficit) {
                                Text(
                                    "Shortfall: -${activePoint.shortfall.toFormattedString()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF5350)
                                )
                            } else {
                                val balLabel = if (chartMode == EntryChartMode.ASSET_POOL) "Bal:" else "Debt:"
                                Text(
                                    "$balLabel ${activePoint.balance.toFormattedString()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }

                        if (activePoint.inflow.value > 0L && chartMode != EntryChartMode.CASH_AVAILABLE) {
                            val inLabel = if (chartMode == EntryChartMode.ASSET_POOL) "Deposit:" else if (chartMode == EntryChartMode.INCOME_STREAM) "Pay:" else "In:"
                            Text(
                                "$inLabel +${activePoint.inflow.toFormattedString()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                        }

                        if (activePoint.outflow.value > 0L && chartMode != EntryChartMode.CASH_AVAILABLE) {
                            val outLabel = if (chartMode == EntryChartMode.ASSET_POOL) "Withdrawal:" else if (chartMode == EntryChartMode.COMPOUNDING_DEBT) "Pmt:" else "Out:"
                            Text(
                                "$outLabel -${activePoint.outflow.toFormattedString()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF5350)
                            )
                        }
                    }
                }
            }
        }

        // X-Axis Year Labels & Dual-Axis Bounds
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val first = points.first()
            val mid = points[points.size / 2]
            val last = points.last()

            Text("${first.calendarYear} (${first.age})", fontSize = 9.sp, color = Color(0xFF888888))
            Text("${mid.calendarYear} (${mid.age})", fontSize = 9.sp, color = Color(0xFF888888))
            Text("${last.calendarYear} (${last.age})", fontSize = 9.sp, color = Color(0xFF888888))
        }
    }
}

@Composable
private fun LegendBadge(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(Color(0xFF262626), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        Text(label, fontSize = 9.sp, color = Color(0xFFCCCCCC), fontWeight = FontWeight.Medium)
    }
}
