package com.muandrew.forecast.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muandrew.forecast.engine.YearCategoryBreakdown
import com.muandrew.forecast.model.AssetCategory
import com.muandrew.forecast.model.Money
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NetWorthChart(
    timeline: List<YearCategoryBreakdown>,
    p10Path: List<Money> = emptyList(),
    p50Path: List<Money> = emptyList(),
    p90Path: List<Money> = emptyList(),
    selectedCategory: AssetCategory? = null,
    onSelectCategory: (AssetCategory?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var hoveredYearIndex by remember { mutableStateOf<Int?>(null) }

    if (timeline.isEmpty()) {
        Box(modifier = modifier.height(320.dp), contentAlignment = Alignment.Center) {
            Text("No simulation data available", color = Color.Gray)
        }
        return
    }

    val maxNetWorthCents = max(
        1L,
        max(
            timeline.maxOfOrNull { it.totalNetWorth.value } ?: 1L,
            p90Path.maxOfOrNull { it.value } ?: 1L,
        ),
    )

    val activeYearIndex = hoveredYearIndex ?: (timeline.size - 1)
    val activeYearData = timeline.getOrNull(activeYearIndex) ?: timeline.last()

    Card(
        backgroundColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        elevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Selected Year Inspection HUD (Responsive Flexbox FlowRow)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        "Stacked Net Worth & Category Growth Over Time",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                    )
                    Text(
                        "Drag or tap chart to inspect at specific ages. Click categories to highlight.",
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA),
                    )
                }

                // Interactive HUD Inspection Badge
                InspectionBadge(
                    activeData = activeYearData,
                    selectedCategory = selectedCategory,
                    p10 = p10Path.getOrNull(activeYearIndex),
                    p90 = p90Path.getOrNull(activeYearIndex),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Category Interactive Legend with Highlighting
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AssetCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    val catColor = Color(category.hexColor)
                    val currentBal = activeYearData.assetBalances[category] ?: Money.ZERO

                    Row(
                        modifier = Modifier
                            .background(
                                if (isSelected) catColor.copy(alpha = 0.25f) else Color(0xFF252525),
                                RoundedCornerShape(6.dp),
                            )
                            .clickable {
                                onSelectCategory(if (isSelected) null else category)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(catColor, CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            category.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                        )
                        if (currentBal.value > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "(${currentBal.toFormattedString()})",
                                fontSize = 10.sp,
                                color = catColor,
                            )
                        }
                    }
                }

                if (selectedCategory != null) {
                    Text(
                        "Reset Highlight",
                        fontSize = 11.sp,
                        color = Color(0xFF81C784),
                        modifier = Modifier
                            .clickable { onSelectCategory(null) }
                            .padding(start = 4.dp, top = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .pointerInput(timeline.size) {
                        detectTapGestures { offset ->
                            val xRatio = (offset.x / size.width).coerceIn(0f, 1f)
                            hoveredYearIndex = (xRatio * (timeline.size - 1)).toInt().coerceIn(0, timeline.size - 1)
                        }
                    }
                    .pointerInput(timeline.size) {
                        detectDragGestures { change, _ ->
                            val xRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                            hoveredYearIndex = (xRatio * (timeline.size - 1)).toInt().coerceIn(0, timeline.size - 1)
                        }
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    val width = size.width
                    val height = size.height
                    val stepX = if (timeline.size > 1) width / (timeline.size - 1) else width

                    // 1. Draw Grid Lines & Y-Axis markers
                    val yTicks = 4
                    for (i in 0..yTicks) {
                        val y = height - (i.toFloat() / yTicks) * height
                        drawLine(
                            color = Color(0xFF333333),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f,
                        )
                    }

                    // 2. Draw Monte Carlo Percentile Cone (P10 to P90)
                    if (p10Path.size == timeline.size && p90Path.size == timeline.size) {
                        val conePath = Path()
                        for (i in timeline.indices) {
                            val x = i * stepX
                            val yTop = height - (p90Path[i].value.toFloat() / maxNetWorthCents) * height
                            if (i == 0) conePath.moveTo(x, yTop) else conePath.lineTo(x, yTop)
                        }
                        for (i in timeline.indices.reversed()) {
                            val x = i * stepX
                            val yBottom = height - (p10Path[i].value.toFloat() / maxNetWorthCents) * height
                            conePath.lineTo(x, yBottom)
                        }
                        conePath.close()
                        drawPath(conePath, color = Color(0xFF64B5F6).copy(alpha = 0.12f))
                    }

                    // 3. Draw Stacked Asset Category Areas
                    val categories = AssetCategory.entries
                    val bottomHeights = FloatArray(timeline.size) { 0f }

                    for (cat in categories) {
                        val catColor = Color(cat.hexColor)
                        val isHighlighted = selectedCategory == null || selectedCategory == cat
                        val alpha = if (isHighlighted) 0.85f else 0.18f

                        val areaPath = Path()
                        val topYCoords = FloatArray(timeline.size)

                        for (i in timeline.indices) {
                            val x = i * stepX
                            val catVal = timeline[i].assetBalances[cat]?.value ?: 0L
                            val startY = height - (bottomHeights[i] / maxNetWorthCents) * height
                            val layerHeight = (catVal.toFloat() / maxNetWorthCents) * height
                            val endY = startY - layerHeight
                            topYCoords[i] = endY

                            if (i == 0) areaPath.moveTo(x, startY)
                        }

                        for (i in timeline.indices) {
                            val x = i * stepX
                            areaPath.lineTo(x, topYCoords[i])
                        }

                        for (i in timeline.indices.reversed()) {
                            val x = i * stepX
                            val startY = height - (bottomHeights[i] / maxNetWorthCents) * height
                            areaPath.lineTo(x, startY)
                        }
                        areaPath.close()

                        drawPath(areaPath, color = catColor.copy(alpha = alpha))

                        if (isHighlighted && selectedCategory == cat) {
                            // Draw glowing top contour for highlighted category
                            val linePath = Path()
                            for (i in timeline.indices) {
                                val x = i * stepX
                                if (i == 0) linePath.moveTo(x, topYCoords[i]) else linePath.lineTo(x, topYCoords[i])
                            }
                            drawPath(linePath, color = catColor, style = Stroke(width = 2.5f))
                        }

                        // Accumulate bottom heights for next category stack
                        for (i in timeline.indices) {
                            val catVal = timeline[i].assetBalances[cat]?.value ?: 0L
                            bottomHeights[i] += catVal.toFloat()
                        }
                    }

                    // 4. Draw Cursor line at active hovered year
                    if (hoveredYearIndex != null && hoveredYearIndex!! in timeline.indices) {
                        val cursorX = hoveredYearIndex!! * stepX
                        drawLine(
                            color = Color.White.copy(alpha = 0.75f),
                            start = Offset(cursorX, 0f),
                            end = Offset(cursorX, height),
                            strokeWidth = 1.5f,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectionBadge(
    activeData: YearCategoryBreakdown,
    selectedCategory: AssetCategory?,
    p10: Money?,
    p90: Money?,
) {
    Box(
        modifier = Modifier
            .background(Color(0xFF242424), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text("${activeData.calendarYear} (Age ${activeData.age})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Year +${activeData.yearIndex}", fontSize = 10.sp, color = Color(0xFFAAAAAA))
            }
            Divider(modifier = Modifier.height(24.dp).width(1.dp), color = Color(0xFF444444))
            Column {
                Text(
                    "Total: ${activeData.totalNetWorth.toFormattedString()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784),
                )
                if (selectedCategory != null) {
                    val catVal = activeData.assetBalances[selectedCategory] ?: Money.ZERO
                    val pct = if (activeData.totalNetWorth.value > 0) {
                        (catVal.value.toDouble() / activeData.totalNetWorth.value.toDouble() * 100.0).toInt()
                    } else {
                        0
                    }
                    Text(
                        "${selectedCategory.displayName}: ${catVal.toFormattedString()} ($pct%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(selectedCategory.hexColor),
                    )
                } else if (p10 != null && p90 != null) {
                    Text(
                        "Risk Band: ${p10.toFormattedString()} – ${p90.toFormattedString()}",
                        fontSize = 10.sp,
                        color = Color(0xFF64B5F6),
                    )
                }
            }
        }
    }
}
