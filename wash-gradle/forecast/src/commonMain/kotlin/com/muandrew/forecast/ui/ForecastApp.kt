package com.muandrew.forecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muandrew.forecast.engine.MonteCarloEngine
import com.muandrew.forecast.engine.RetirementCalculator
import com.muandrew.forecast.model.ForecastProfile
import com.muandrew.forecast.model.Money
import com.muandrew.forecast.model.MonteCarloResult
import com.muandrew.forecast.model.YearTrajectory

private val DarkColors = darkColors(
    primary = Color(0xFF64B5F6),
    primaryVariant = Color(0xFF1E88E5),
    secondary = Color(0xFF81C784),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFEEEEEE)
)

@Composable
fun ForecastApp() {
    MaterialTheme(colors = DarkColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            var currentAgeText by remember { mutableStateOf("30") }
            var retirementAgeText by remember { mutableStateOf("60") }
            var netWorthText by remember { mutableStateOf("100000") }
            var annualSavingsText by remember { mutableStateOf("25000") }
            var annualExpensesText by remember { mutableStateOf("60000") }
            var expectedReturnText by remember { mutableStateOf("7.0") }
            var inflationText by remember { mutableStateOf("2.5") }
            var volatilityText by remember { mutableStateOf("15.0") }

            var simulationResult by remember {
                mutableStateOf<MonteCarloResult?>(
                    MonteCarloEngine.runSimulation(
                        ForecastProfile(),
                        simulationsCount = 1000,
                        randomSeed = 42L
                    )
                )
            }

            fun buildProfile(): ForecastProfile {
                val cAge = currentAgeText.toIntOrNull() ?: 30
                val rAge = retirementAgeText.toIntOrNull() ?: 60
                val nw = netWorthText.toLongOrNull() ?: 100000L
                val savings = annualSavingsText.toLongOrNull() ?: 25000L
                val expenses = annualExpensesText.toLongOrNull() ?: 60000L
                val retRate = (expectedReturnText.toDoubleOrNull() ?: 7.0) / 100.0
                val infRate = (inflationText.toDoubleOrNull() ?: 2.5) / 100.0
                val volRate = (volatilityText.toDoubleOrNull() ?: 15.0) / 100.0

                return ForecastProfile(
                    currentAge = cAge,
                    retirementAge = rAge,
                    lifeExpectancy = 90,
                    currentNetWorth = Money.ofDollars(nw),
                    annualSavings = Money.ofDollars(savings),
                    annualRetirementExpenses = Money.ofDollars(expenses),
                    expectedReturnRate = retRate,
                    inflationRate = infRate,
                    returnVolatility = volRate
                )
            }

            val currentProfile = buildProfile()
            val swrAnalysis = RetirementCalculator.calculateSWR(currentProfile)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    HeaderSection()
                }

                item {
                    ParametersCard(
                        currentAgeText = currentAgeText,
                        onCurrentAgeChange = { currentAgeText = it },
                        retirementAgeText = retirementAgeText,
                        onRetirementAgeChange = { retirementAgeText = it },
                        netWorthText = netWorthText,
                        onNetWorthChange = { netWorthText = it },
                        annualSavingsText = annualSavingsText,
                        onAnnualSavingsChange = { annualSavingsText = it },
                        annualExpensesText = annualExpensesText,
                        onAnnualExpensesChange = { annualExpensesText = it },
                        expectedReturnText = expectedReturnText,
                        onExpectedReturnChange = { expectedReturnText = it },
                        inflationText = inflationText,
                        onInflationChange = { inflationText = it },
                        volatilityText = volatilityText,
                        onVolatilityChange = { volatilityText = it },
                        onRunSimulation = {
                            simulationResult = MonteCarloEngine.runSimulation(
                                buildProfile(),
                                simulationsCount = 1000
                            )
                        }
                    )
                }

                item {
                    SummaryMetricsCard(
                        result = simulationResult,
                        swr = swrAnalysis,
                        profile = currentProfile
                    )
                }

                item {
                    Text(
                        "Wealth Trajectory Percentile Projections (Real Inflation-Adjusted)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                val trajectory = simulationResult?.trajectory ?: emptyList()
                // Show milestone years
                val keyYears = trajectory.filter { traj ->
                    traj.yearIndex == 0 ||
                    traj.age == currentProfile.retirementAge ||
                    traj.age % 5 == 0 ||
                    traj.age == currentProfile.lifeExpectancy
                }

                item {
                    TrajectoryTableHeader()
                }

                items(keyYears) { yearData ->
                    TrajectoryTableRow(
                        data = yearData,
                        isRetirementAge = yearData.age == currentProfile.retirementAge
                    )
                }

                item {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column {
        Text(
            text = "Financial Planning & Wealth Forecast",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )
        Text(
            text = "Wasm-First Kotlin Multiplatform Simulation Engine (Monte Carlo & Safe Withdrawal Rates)",
            fontSize = 14.sp,
            color = Color(0xFFAAAAAA)
        )
    }
}

@Composable
private fun ParametersCard(
    currentAgeText: String,
    onCurrentAgeChange: (String) -> Unit,
    retirementAgeText: String,
    onRetirementAgeChange: (String) -> Unit,
    netWorthText: String,
    onNetWorthChange: (String) -> Unit,
    annualSavingsText: String,
    onAnnualSavingsChange: (String) -> Unit,
    annualExpensesText: String,
    onAnnualExpensesChange: (String) -> Unit,
    expectedReturnText: String,
    onExpectedReturnChange: (String) -> Unit,
    inflationText: String,
    onInflationChange: (String) -> Unit,
    volatilityText: String,
    onVolatilityChange: (String) -> Unit,
    onRunSimulation: () -> Unit
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Simulation Parameters", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentAgeText,
                    onValueChange = onCurrentAgeChange,
                    label = { Text("Current Age") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = retirementAgeText,
                    onValueChange = onRetirementAgeChange,
                    label = { Text("Retirement Age") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = netWorthText,
                    onValueChange = onNetWorthChange,
                    label = { Text("Current Net Worth ($)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = annualSavingsText,
                    onValueChange = onAnnualSavingsChange,
                    label = { Text("Annual Savings ($)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = annualExpensesText,
                    onValueChange = onAnnualExpensesChange,
                    label = { Text("Retirement Expenses ($)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = expectedReturnText,
                    onValueChange = onExpectedReturnChange,
                    label = { Text("Expected Return (%)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = inflationText,
                    onValueChange = onInflationChange,
                    label = { Text("Inflation Rate (%)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = volatilityText,
                    onValueChange = onVolatilityChange,
                    label = { Text("Market Volatility (%)") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onRunSimulation,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Text("Re-Run Simulation", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricsCard(
    result: MonteCarloResult?,
    swr: com.muandrew.forecast.model.SWRAnalysis,
    profile: ForecastProfile
) {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Forecast Analysis & Retirement Readiness", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val successRate = result?.successRate ?: 0.0
                val successColor = if (successRate >= 85.0) Color(0xFF81C784) else if (successRate >= 65.0) Color(0xFFFFB74D) else Color(0xFFE57373)

                MetricBox(
                    label = "Monte Carlo Success Rate",
                    value = "${(successRate * 10).toInt() / 10.0}%",
                    valueColor = successColor,
                    subtitle = "1,000 stochastic runs to Age 90"
                )

                MetricBox(
                    label = "Target SWR Portfolio (4%)",
                    value = swr.targetPortfolioSize.toFormattedString(),
                    valueColor = MaterialTheme.colors.primary,
                    subtitle = "Required for ${swr.annualExpenseTarget.toFormattedString()}/yr"
                )

                MetricBox(
                    label = "Projected at Age ${profile.retirementAge}",
                    value = swr.projectedPortfolioAtRetirement.toFormattedString(),
                    valueColor = if (swr.isRetirementFunded) Color(0xFF81C784) else Color(0xFFFFB74D),
                    subtitle = "Funding Ratio: ${(swr.fundingRatio * 100).toInt()}%"
                )

                MetricBox(
                    label = "Median Net Worth (Age 90)",
                    value = result?.finalMedianNetWorth?.toFormattedString() ?: "$0.00",
                    valueColor = Color.White,
                    subtitle = "P10: ${result?.p10FinalNetWorth?.toFormattedString()} | P90: ${result?.p90FinalNetWorth?.toFormattedString()}"
                )
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, valueColor: Color, subtitle: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .width(220.dp)
    ) {
        Column {
            Text(label, fontSize = 12.sp, color = Color(0xFFAAAAAA))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = Color(0xFF888888))
        }
    }
}

@Composable
private fun TrajectoryTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF252525), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Age", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
        Text("P10 (Pessimistic)", fontWeight = FontWeight.Bold, color = Color(0xFFE57373), modifier = Modifier.weight(2f))
        Text("P25", fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D), modifier = Modifier.weight(2f))
        Text("P50 (Median)", fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6), modifier = Modifier.weight(2f))
        Text("P75", fontWeight = FontWeight.Bold, color = Color(0xFF81C784), modifier = Modifier.weight(2f))
        Text("P90 (Optimistic)", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), modifier = Modifier.weight(2f))
    }
}

@Composable
private fun TrajectoryTableRow(data: YearTrajectory, isRetirementAge: Boolean) {
    val bgColor = if (isRetirementAge) Color(0xFF1B3A4B) else Color(0xFF1E1E1E)
    val borderModifier = if (isRetirementAge) Modifier.border(1.dp, Color(0xFF64B5F6)) else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ageLabel = if (isRetirementAge) "Age ${data.age} ★" else "Age ${data.age}"
        Text(ageLabel, color = if (isRetirementAge) Color(0xFF64B5F6) else Color.White, fontWeight = if (isRetirementAge) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        Text(data.balanceP10.toFormattedString(), color = Color(0xFFE57373), modifier = Modifier.weight(2f))
        Text(data.balanceP25.toFormattedString(), color = Color(0xFFFFB74D), modifier = Modifier.weight(2f))
        Text(data.balanceP50.toFormattedString(), color = Color(0xFF64B5F6), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2f))
        Text(data.balanceP75.toFormattedString(), color = Color(0xFF81C784), modifier = Modifier.weight(2f))
        Text(data.balanceP90.toFormattedString(), color = Color(0xFF4CAF50), modifier = Modifier.weight(2f))
    }
    Divider(color = Color(0xFF2E2E2E), thickness = 1.dp)
}
