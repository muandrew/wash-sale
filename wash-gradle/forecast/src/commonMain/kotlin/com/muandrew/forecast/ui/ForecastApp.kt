package com.muandrew.forecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
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
import kotlin.math.max
import kotlin.math.min
import com.muandrew.forecast.engine.MultiAssetEngine
import com.muandrew.forecast.engine.YearCategoryBreakdown
import com.muandrew.forecast.model.AssetCategory
import com.muandrew.forecast.model.AssetPool
import com.muandrew.forecast.model.Entity
import com.muandrew.forecast.model.ExpenseCategory
import com.muandrew.forecast.model.ExpenseItem
import com.muandrew.forecast.model.ExpenseType
import com.muandrew.forecast.model.FinancialPlan
import com.muandrew.forecast.model.FundingStatus
import com.muandrew.forecast.model.Household
import com.muandrew.forecast.model.IncomeStream
import com.muandrew.forecast.model.Money
import com.muandrew.forecast.model.PriorityItemType
import com.muandrew.forecast.model.PriorityRule
import com.muandrew.forecast.model.PriorityTargetType
import com.muandrew.forecast.model.SchedulePhase
import com.muandrew.forecast.model.TimeMode
import com.muandrew.forecast.model.YearlyItemFunding

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

enum class AppTab(val title: String) {
    OVERVIEW("Overview & Chart"),
    FINANCIAL_STREAMS("Income, Expenses & Pools"),
    CASHFLOW_PRIORITY("Cashflow & Priority"),
    ENTITIES("Entities & Households")
}

enum class CashflowFilterType(val title: String) {
    ALL("All Streams & Pools"),
    INCOME_ONLY("Income Only"),
    POOLS_ONLY("Asset Pools / Investments"),
    EXPENSES_ONLY("Expenses & Debt")
}

@Composable
fun ForecastApp() {
    MaterialTheme(colors = DarkColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            val basePlanYear = 2026

            // Initial default household state with entities, income streams, asset pools with multi-phase schedules, and expenses
            var primaryHousehold by remember {
                mutableStateOf(
                    Household(
                        id = "primary",
                        name = "Primary Household",
                        isPrimary = true,
                        baseYear = basePlanYear,
                        entities = listOf(
                            Entity("entity_primary", "Primary Earner (Alex)", birthYear = 1996, isPrimary = true, retirementAge = 60, lifeExpectancy = 90),
                            Entity("entity_child", "Emma (Child)", birthYear = 2028, isPrimary = false, retirementAge = 65, lifeExpectancy = 90)
                        ),
                        incomeStreams = listOf(
                            IncomeStream(
                                id = "inc_primary_job",
                                name = "Primary Career Salary",
                                entityId = "entity_primary",
                                initialAnnualAmount = Money.ofDollars(140_000),
                                timeMode = TimeMode.ENTITY_AGE,
                                startAge = 30,
                                endAge = 60,
                                startYear = 2026,
                                endYear = 2056,
                                yearlyPayBumpRate = 0.035
                            )
                        ),
                        assetPools = listOf(
                            AssetPool(
                                id = "p_taxable",
                                name = "Taxable Brokerage",
                                category = AssetCategory.TAXABLE_BROKERAGE,
                                currentBalance = Money.ofDollars(50_000),
                                entityId = "entity_primary",
                                expectedNominalReturn = 0.075,
                                annualContribution = Money.ofDollars(15_000),
                                phases = listOf(
                                    SchedulePhase("phase_tax_1", "Ages 30–40", TimeMode.ENTITY_AGE, startAge = 30, endAge = 40, startYear = 2026, endYear = 2036, amount = Money.ofDollars(12_000), isWithdrawal = false),
                                    SchedulePhase("phase_tax_2", "Ages 41–60", TimeMode.ENTITY_AGE, startAge = 41, endAge = 60, startYear = 2037, endYear = 2056, amount = Money.ofDollars(20_000), isWithdrawal = false),
                                    SchedulePhase("phase_tax_3", "Ages 61+ (Retirement Drawdown)", TimeMode.ENTITY_AGE, startAge = 61, endAge = 85, startYear = 2057, endYear = 2081, amount = Money.ofDollars(35_000), isWithdrawal = true)
                                )
                            ),
                            AssetPool("p_401k", "Workplace 401(k)", AssetCategory.PRE_TAX_401K, Money.ofDollars(40_000), entityId = "entity_primary", expectedNominalReturn = 0.070, annualContribution = Money.ofDollars(23_000)),
                            AssetPool("p_roth", "Roth IRA", AssetCategory.ROTH_IRA, Money.ofDollars(20_000), entityId = "entity_primary", expectedNominalReturn = 0.075, annualContribution = Money.ofDollars(7_000)),
                            AssetPool("p_cash", "Emergency HYSA", AssetCategory.CASH_EMERGENCY, Money.ofDollars(25_000), entityId = "entity_primary", expectedNominalReturn = 0.035, annualContribution = Money.ofDollars(2_000)),
                            AssetPool(
                                id = "p_529",
                                name = "Emma's 529 College Fund",
                                category = AssetCategory.TAXABLE_BROKERAGE,
                                currentBalance = Money.ofDollars(10_000),
                                entityId = "entity_child",
                                expectedNominalReturn = 0.070,
                                annualContribution = Money.ofDollars(6_000),
                                contributionEndAge = 18,
                                phases = listOf(
                                    SchedulePhase("phase_529_save", "Ages 0–17 (Contributions)", TimeMode.ENTITY_AGE, startAge = 0, endAge = 17, startYear = 2028, endYear = 2045, amount = Money.ofDollars(6_000), isWithdrawal = false),
                                    SchedulePhase("phase_529_draw", "Ages 18–21 (College Tuition Drawdown)", TimeMode.ENTITY_AGE, startAge = 18, endAge = 21, startYear = 2046, endYear = 2049, amount = Money.ofDollars(25_000), isWithdrawal = true)
                                )
                            )
                        ),
                        expenses = listOf(
                            ExpenseItem("e_living", "Baseline Living Essentials", ExpenseCategory.LIVING_ESSENTIALS, Money.ofDollars(45_000), entityId = "entity_primary", timeMode = TimeMode.ENTITY_AGE, startAge = 30, endAge = 90, startYear = 2026, endYear = 2086, expenseType = ExpenseType.RECURRING),
                            ExpenseItem("e_vacation", "Annual Vacation Budget", ExpenseCategory.DISCRETIONARY_VACATION, Money.ofDollars(8_000), entityId = "entity_primary", timeMode = TimeMode.ENTITY_AGE, startAge = 30, endAge = 85, startYear = 2026, endYear = 2081, expenseType = ExpenseType.RECURRING),
                            ExpenseItem("e_daycare", "Child Daycare (Ages 0-5)", ExpenseCategory.CHILDCARE_EARLY, Money.ofDollars(18_000), entityId = "entity_child", timeMode = TimeMode.ENTITY_AGE, startAge = 0, endAge = 5, startYear = 2028, endYear = 2033, expenseType = ExpenseType.RECURRING),
                            ExpenseItem("e_college", "College Tuition (Ages 18-21)", ExpenseCategory.EDUCATION_TUITION, Money.ofDollars(35_000), entityId = "entity_child", timeMode = TimeMode.ENTITY_AGE, startAge = 18, endAge = 21, startYear = 2046, endYear = 2049, expenseType = ExpenseType.RECURRING),
                            ExpenseItem("e_debt", "Student / Personal Loan", ExpenseCategory.HOUSING_MORTGAGE, Money.ofDollars(6_000), entityId = "entity_primary", timeMode = TimeMode.CALENDAR_YEAR, startAge = 30, endAge = 35, startYear = 2026, endYear = 2031, expenseType = ExpenseType.COMPOUNDING_DEBT, compoundingInterestRate = 0.06)
                        )
                    )
                )
            }

            var elderCareHousehold by remember {
                mutableStateOf<Household?>(null)
            }

            var selectedHouseholdId by remember { mutableStateOf("consolidated") }
            var selectedAssetCategory by remember { mutableStateOf<AssetCategory?>(null) }
            var currentTab by remember { mutableStateOf(AppTab.OVERVIEW) }

            // Active FinancialPlan
            val householdsList = listOfNotNull(primaryHousehold, elderCareHousehold)
            val plan = FinancialPlan(
                id = "main_plan",
                name = "Comprehensive Wealth Plan",
                baseYear = basePlanYear,
                households = householdsList,
                inflationRate = 0.025
            )

            // Run simulation engine
            val planResult = remember(plan) {
                MultiAssetEngine.simulatePlan(plan, simulationsCount = 500)
            }

            // Determine active timeline for chart display
            val activeResult = if (selectedHouseholdId == "consolidated" || elderCareHousehold == null) {
                planResult
            } else {
                val hResult = planResult.householdResults.firstOrNull { it.householdId == selectedHouseholdId }
                if (hResult != null) {
                    com.muandrew.forecast.engine.ConsolidatedPlanResult(
                        timeline = hResult.timeline,
                        householdResults = listOf(hResult),
                        finalNetWorth = hResult.finalNetWorth,
                        p10Path = hResult.p10Path,
                        p50Path = hResult.p50Path,
                        p90Path = hResult.p90Path,
                        overallSuccessRate = hResult.successRate
                    )
                } else planResult
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeaderSection()
                }

                // Household Switcher Bar
                item {
                    HouseholdSwitcher(
                        households = householdsList,
                        selectedId = selectedHouseholdId,
                        onSelectId = { selectedHouseholdId = it },
                        onAddElderCare = {
                            elderCareHousehold = Household(
                                id = "elder_care",
                                name = "Aging Parents Support",
                                isPrimary = false,
                                baseYear = basePlanYear,
                                entities = listOf(
                                    Entity("entity_parent", "Robert (Parent)", birthYear = 1961, isPrimary = true, retirementAge = 65, lifeExpectancy = 90)
                                ),
                                incomeStreams = listOf(
                                    IncomeStream("inc_parents_ss", "Social Security & Pension", Money.ofDollars(28_000), entityId = "entity_parent", timeMode = TimeMode.CALENDAR_YEAR, startAge = 65, endAge = 90, startYear = 2026, endYear = 2051, yearlyPayBumpRate = 0.0)
                                ),
                                assetPools = listOf(
                                    AssetPool("ec_cash", "Parents Savings", AssetCategory.CASH_EMERGENCY, Money.ofDollars(50_000), entityId = "entity_parent", expectedNominalReturn = 0.035),
                                    AssetPool("ec_ira", "Parents Traditional IRA", AssetCategory.PRE_TAX_401K, Money.ofDollars(120_000), entityId = "entity_parent", expectedNominalReturn = 0.055)
                                ),
                                expenses = listOf(
                                    ExpenseItem("ec_care", "Assisted Living & Healthcare", ExpenseCategory.HEALTHCARE, Money.ofDollars(35_000), entityId = "entity_parent", timeMode = TimeMode.CALENDAR_YEAR, startAge = 65, endAge = 90, startYear = 2026, endYear = 2051, expenseType = ExpenseType.RECURRING)
                                )
                            )
                        }
                    )
                }

                // Navigation Tabs
                item {
                    TabRow(
                        selectedTabIndex = currentTab.ordinal,
                        backgroundColor = Color(0xFF1E1E1E),
                        contentColor = MaterialTheme.colors.primary
                    ) {
                        AppTab.entries.forEach { tab ->
                            Tab(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                text = { Text(tab.title, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }

                when (currentTab) {
                    AppTab.OVERVIEW -> {
                        item {
                            SummaryMetricsGrid(
                                activeResult = activeResult,
                                onNavigateToPriority = { currentTab = AppTab.CASHFLOW_PRIORITY }
                            )
                        }

                        // Budget Balancer Banner if any years have shortfalls
                        val problemYears = activeResult.timeline.filter { it.unfundedCount > 0 || it.netCashFlow.value < 0 }
                        if (problemYears.isNotEmpty()) {
                            item {
                                BudgetBalancerBanner(
                                    problemYears = problemYears,
                                    household = primaryHousehold,
                                    onUpdateHousehold = { primaryHousehold = it }
                                )
                            }
                        }

                        item {
                            NetWorthChart(
                                timeline = activeResult.timeline,
                                p10Path = activeResult.p10Path,
                                p50Path = activeResult.p50Path,
                                p90Path = activeResult.p90Path,
                                selectedCategory = selectedAssetCategory,
                                onSelectCategory = { selectedAssetCategory = it }
                            )
                        }

                        item {
                            MilestoneBreakdownSection(
                                timeline = activeResult.timeline
                            )
                        }
                    }

                    AppTab.FINANCIAL_STREAMS -> {
                        item {
                            FinancialStreamsManager(
                                household = primaryHousehold,
                                onUpdateHousehold = { primaryHousehold = it }
                            )
                        }
                    }

                    AppTab.CASHFLOW_PRIORITY -> {
                        item {
                            CashflowPriorityManager(
                                household = primaryHousehold,
                                timeline = activeResult.timeline,
                                onUpdateHousehold = { primaryHousehold = it }
                            )
                        }
                    }

                    AppTab.ENTITIES -> {
                        item {
                            EntitiesAndHouseholdsManager(
                                primaryHousehold = primaryHousehold,
                                elderCareHousehold = elderCareHousehold,
                                onUpdatePrimary = { primaryHousehold = it },
                                onUpdateElderCare = { elderCareHousehold = it }
                            )
                        }
                    }
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
            text = "Unified Streams & Compounding Pools, Multi-Phase Schedules & Interactive Budget Balancer",
            fontSize = 13.sp,
            color = Color(0xFFAAAAAA)
        )
    }
}

@Composable
private fun HouseholdSwitcher(
    households: List<Household>,
    selectedId: String,
    onSelectId: (String) -> Unit,
    onAddElderCare: () -> Unit
) {
    Card(
        backgroundColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Entity View:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)

            FilterChip(
                label = "Consolidated (${households.size} Entities)",
                isSelected = selectedId == "consolidated",
                onClick = { onSelectId("consolidated") }
            )

            households.forEach { h ->
                FilterChip(
                    label = h.name,
                    isSelected = selectedId == h.id,
                    onClick = { onSelectId(h.id) }
                )
            }

            if (households.none { it.id == "elder_care" }) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onAddElderCare,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81C784))
                ) {
                    Text("+ Add Aging Parents Entity", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.25f) else Color(0xFF2A2A2A),
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colors.primary else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFFAAAAAA)
        )
    }
}

@Composable
private fun SummaryMetricsGrid(
    activeResult: com.muandrew.forecast.engine.ConsolidatedPlanResult,
    onNavigateToPriority: () -> Unit
) {
    val finalNW = activeResult.finalNetWorth
    val p10 = activeResult.p10Path.lastOrNull() ?: Money.ZERO
    val p90 = activeResult.p90Path.lastOrNull() ?: Money.ZERO
    val success = activeResult.overallSuccessRate

    val totalUnderfundedYears = activeResult.timeline.count { it.unfundedCount > 0 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Monte Carlo Success Rate",
            value = "${(success * 10).toInt() / 10.0}%",
            subtitle = "500 multi-asset stochastic runs",
            color = if (success >= 85.0) Color(0xFF81C784) else Color(0xFFFFB74D),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Ending Median Net Worth",
            value = finalNW.toFormattedString(),
            subtitle = "P10: ${p10.toFormattedString()} | P90: ${p90.toFormattedString()}",
            color = MaterialTheme.colors.primary,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Funding Waterfall Health",
            value = if (totalUnderfundedYears == 0) "100% Funded" else "$totalUnderfundedYears Years Short",
            subtitle = if (totalUnderfundedYears == 0) "All investments & payouts covered" else "Click to inspect & balance budget",
            color = if (totalUnderfundedYears == 0) Color(0xFF81C784) else Color(0xFFEF5350),
            onClick = if (totalUnderfundedYears > 0) onNavigateToPriority else null,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = Color(0xFFAAAAAA))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = Color(0xFF888888))
        }
    }
}

/**
 * Interactive Budget Balancer for Unbalanced Years
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetBalancerBanner(
    problemYears: List<YearCategoryBreakdown>,
    household: Household,
    onUpdateHousehold: (Household) -> Unit
) {
    var selectedYearIdx by remember { mutableStateOf(0) }
    val currentProblem = problemYears.getOrNull(selectedYearIdx) ?: problemYears.first()

    val totalShortfallCents = currentProblem.itemFundings.sumOf { it.shortfall.value }

    Card(
        backgroundColor = Color(0xFF381515),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚡ Budget Balancer: Year ${currentProblem.calendarYear} (Age ${currentProblem.age})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF5350), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SHORTFALL: -${Money(totalShortfallCents).toFormattedString()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                // Year Switcher if multiple problem years
                if (problemYears.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        problemYears.forEachIndexed { idx, yr ->
                            FilterChip(
                                label = "${yr.calendarYear}",
                                isSelected = selectedYearIdx == idx,
                                onClick = { selectedYearIdx = idx }
                            )
                        }
                    }
                }
            }

            Text(
                "In Year ${currentProblem.calendarYear}, income is ${currentProblem.totalIncome.toFormattedString()} while total expenses and planned investments require ${currentProblem.totalExpenses.toFormattedString()}. Click an action below to balance this year's budget:",
                fontSize = 12.sp,
                color = Color(0xFFEEEEEE)
            )

            // One-Click Adjuster Buttons
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Boost Income
                Button(
                    onClick = {
                        val primStream = household.incomeStreams.firstOrNull()
                        if (primStream != null) {
                            val newAmount = primStream.initialAnnualAmount + Money(totalShortfallCents)
                            val updated = household.incomeStreams.map {
                                if (it.id == primStream.id) it.copy(initialAnnualAmount = newAmount) else it
                            }
                            onUpdateHousehold(household.copy(incomeStreams = updated))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF81C784))
                ) {
                    Text("+ Boost Career Pay by +${Money(totalShortfallCents).toFormattedString()}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // 2. Trim Discretionary Vacation
                val vacationExp = household.expenses.firstOrNull { it.category == ExpenseCategory.DISCRETIONARY_VACATION }
                if (vacationExp != null && vacationExp.annualAmount.value > 0) {
                    Button(
                        onClick = {
                            val reduction = min(vacationExp.annualAmount.value, totalShortfallCents)
                            val newAmount = Money(vacationExp.annualAmount.value - reduction)
                            val updated = household.expenses.map {
                                if (it.id == vacationExp.id) it.copy(annualAmount = newAmount) else it
                            }
                            onUpdateHousehold(household.copy(expenses = updated))
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF64B5F6))
                    ) {
                        Text("- Trim Vacation Expense by -${Money(totalShortfallCents).toFormattedString()}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // 3. Add Phase Override for Pool Contribution
                val starvedPool = currentProblem.itemFundings.firstOrNull { it.targetType == PriorityTargetType.POOL_CONTRIBUTION && it.status.isProblem }
                if (starvedPool != null) {
                    Button(
                        onClick = {
                            val pool = household.allAssetPools().firstOrNull { it.id == starvedPool.id }
                            if (pool != null) {
                                val affordablePhase = SchedulePhase(
                                    id = "phase_${currentProblem.calendarYear}",
                                    name = "Adjusted Year ${currentProblem.calendarYear}",
                                    timeMode = TimeMode.CALENDAR_YEAR,
                                    startYear = currentProblem.calendarYear,
                                    endYear = currentProblem.calendarYear,
                                    amount = starvedPool.actualAmount
                                )
                                val updatedPools = household.assetPools.map {
                                    if (it.id == pool.id) it.copy(phases = it.phases + affordablePhase) else it
                                }
                                onUpdateHousehold(household.copy(assetPools = updatedPools))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFA726))
                    ) {
                        Text("Scale Contribution for ${currentProblem.calendarYear} to ${starvedPool.actualAmount.toFormattedString()}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneBreakdownSection(
    timeline: List<YearCategoryBreakdown>
) {
    Card(
        backgroundColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Milestone Year Category Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            val milestones = timeline.filter { it.yearIndex == 0 || it.age % 10 == 0 || it == timeline.last() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF282828), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Year / Age", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1.5f))
                Text("Total Net Worth", fontWeight = FontWeight.Bold, color = Color(0xFF81C784), modifier = Modifier.weight(2f))
                Text("Brokerage", fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5), modifier = Modifier.weight(2f))
                Text("401(k) / IRA", fontWeight = FontWeight.Bold, color = Color(0xFF66BB6A), modifier = Modifier.weight(2f))
                Text("Roth IRA", fontWeight = FontWeight.Bold, color = Color(0xFFAB47BC), modifier = Modifier.weight(2f))
                Text("Cash / Other", fontWeight = FontWeight.Bold, color = Color(0xFFFFCA28), modifier = Modifier.weight(2f))
            }

            milestones.forEach { row ->
                val brokerage = row.assetBalances[AssetCategory.TAXABLE_BROKERAGE] ?: Money.ZERO
                val preTax = row.assetBalances[AssetCategory.PRE_TAX_401K] ?: Money.ZERO
                val roth = row.assetBalances[AssetCategory.ROTH_IRA] ?: Money.ZERO
                val cashOther = (row.assetBalances[AssetCategory.CASH_EMERGENCY] ?: Money.ZERO) + (row.assetBalances[AssetCategory.OTHER] ?: Money.ZERO)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${row.calendarYear} (Age ${row.age})", fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1.5f))
                    Text(row.totalNetWorth.toFormattedString(), fontWeight = FontWeight.Bold, color = Color(0xFF81C784), modifier = Modifier.weight(2f))
                    Text(brokerage.toFormattedString(), color = Color(0xFF42A5F5), modifier = Modifier.weight(2f))
                    Text(preTax.toFormattedString(), color = Color(0xFF66BB6A), modifier = Modifier.weight(2f))
                    Text(roth.toFormattedString(), color = Color(0xFFAB47BC), modifier = Modifier.weight(2f))
                    Text(cashOther.toFormattedString(), color = Color(0xFFFFCA28), modifier = Modifier.weight(2f))
                }
                Divider(color = Color(0xFF2A2A2A), thickness = 1.dp)
            }
        }
    }
}

/**
 * Unified Financial Streams Manager: Income, Compounding Asset Pools & Expenses/Debt
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinancialStreamsManager(
    household: Household,
    onUpdateHousehold: (Household) -> Unit
) {
    var streamFilter by remember { mutableStateOf(CashflowFilterType.ALL) }
    var selectedEntityFilter by remember { mutableStateOf<String?>(null) }

    Card(
        backgroundColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header & Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Income, Expenses & Compounding Asset Pools", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Unified control over income streams, expenses/debt, and compounding asset pools with year-to-year schedules.",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }

            // Quick Add Preset Buttons
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Add Income Stream
                Button(
                    onClick = {
                        val primary = household.primaryEntity()
                        val newId = "income_${household.incomeStreams.size + 1}"
                        val newStream = IncomeStream(
                            id = newId,
                            name = "Additional Career Income",
                            entityId = primary.id,
                            initialAnnualAmount = Money.ofDollars(60_000),
                            timeMode = TimeMode.ENTITY_AGE,
                            startAge = primary.ageInYear(household.baseYear),
                            endAge = primary.retirementAge,
                            startYear = household.baseYear,
                            endYear = primary.yearAtAge(primary.retirementAge),
                            yearlyPayBumpRate = 0.035
                        )
                        onUpdateHousehold(household.copy(incomeStreams = household.incomeStreams + newStream))
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF81C784))
                ) {
                    Text("+ Add Income Stream", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // 2. Add Asset Pool (Compounding with last year's number)
                Button(
                    onClick = {
                        val primary = household.primaryEntity()
                        val newId = "pool_${household.assetPools.size + 1}"
                        val newPool = AssetPool(
                            id = newId,
                            name = "New Investment Account",
                            category = AssetCategory.TAXABLE_BROKERAGE,
                            currentBalance = Money.ofDollars(10_000),
                            entityId = primary.id,
                            expectedNominalReturn = 0.07,
                            annualContribution = Money.ofDollars(5_000)
                        )
                        onUpdateHousehold(household.copy(assetPools = household.assetPools + newPool))
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF42A5F5))
                ) {
                    Text("+ Add Asset Pool (Compounding)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // 3. Add Recurring Expense
                Button(
                    onClick = {
                        val primary = household.primaryEntity()
                        val newId = "exp_rec_${household.expenses.size + 1}"
                        val newItem = ExpenseItem(
                            id = newId,
                            name = "New Living / Essential Expense",
                            category = ExpenseCategory.LIVING_ESSENTIALS,
                            annualAmount = Money.ofDollars(12_000),
                            entityId = primary.id,
                            timeMode = TimeMode.ENTITY_AGE,
                            startAge = primary.ageInYear(household.baseYear),
                            endAge = primary.retirementAge,
                            startYear = household.baseYear,
                            endYear = primary.yearAtAge(primary.retirementAge),
                            expenseType = ExpenseType.RECURRING
                        )
                        onUpdateHousehold(household.copy(expenses = household.expenses + newItem))
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF64B5F6))
                ) {
                    Text("+ Add Recurring Expense", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // 4. Add Compounding Debt
                Button(
                    onClick = {
                        val primary = household.primaryEntity()
                        val newId = "exp_debt_${household.expenses.size + 1}"
                        val newItem = ExpenseItem(
                            id = newId,
                            name = "Loan / Compounding Debt",
                            category = ExpenseCategory.HOUSING_MORTGAGE,
                            annualAmount = Money.ofDollars(8_000),
                            entityId = primary.id,
                            timeMode = TimeMode.CALENDAR_YEAR,
                            startAge = primary.ageInYear(household.baseYear),
                            endAge = primary.ageInYear(household.baseYear + 5),
                            startYear = household.baseYear,
                            endYear = household.baseYear + 5,
                            expenseType = ExpenseType.COMPOUNDING_DEBT,
                            compoundingInterestRate = 0.07
                        )
                        onUpdateHousehold(household.copy(expenses = household.expenses + newItem))
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF5350))
                ) {
                    Text("+ Add Compounding Debt", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // 5. Add Child Expenses
                Button(
                    onClick = {
                        val childEntity = household.entities.firstOrNull { it.id.contains("child") }
                            ?: household.entities.firstOrNull { !it.isPrimary }
                            ?: household.primaryEntity()
                        val childExpenses = listOf(
                            ExpenseItem("child_daycare_${household.expenses.size}", "${childEntity.name} - Daycare (Ages 0-5)", ExpenseCategory.CHILDCARE_EARLY, Money.ofDollars(18_000), entityId = childEntity.id, timeMode = TimeMode.ENTITY_AGE, startAge = 0, endAge = 5, startYear = childEntity.birthYear, endYear = childEntity.birthYear + 5, expenseType = ExpenseType.RECURRING),
                            ExpenseItem("child_college_${household.expenses.size}", "${childEntity.name} - College Tuition (Ages 18-21)", ExpenseCategory.EDUCATION_TUITION, Money.ofDollars(35_000), entityId = childEntity.id, timeMode = TimeMode.ENTITY_AGE, startAge = 18, endAge = 21, startYear = childEntity.birthYear + 18, endYear = childEntity.birthYear + 21, expenseType = ExpenseType.RECURRING)
                        )
                        onUpdateHousehold(household.copy(expenses = household.expenses + childExpenses))
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFA726))
                ) {
                    Text("+ Add Child Lifecycle", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Divider(color = Color(0xFF333333))

            // Filters Bar: Stream Type Filter & Entity Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Filter Chips
                CashflowFilterType.entries.forEach { type ->
                    val count = when (type) {
                        CashflowFilterType.ALL -> household.incomeStreams.size + household.assetPools.size + household.expenses.size
                        CashflowFilterType.INCOME_ONLY -> household.incomeStreams.size
                        CashflowFilterType.POOLS_ONLY -> household.assetPools.size
                        CashflowFilterType.EXPENSES_ONLY -> household.expenses.size
                    }
                    FilterChip(
                        label = "${type.title} ($count)",
                        isSelected = streamFilter == type,
                        onClick = { streamFilter = type }
                    )
                }

                Divider(modifier = Modifier.height(20.dp).width(1.dp), color = Color(0xFF444444))

                // Entity Filter Chips
                FilterChip(
                    label = "All Entities",
                    isSelected = selectedEntityFilter == null,
                    onClick = { selectedEntityFilter = null }
                )

                household.entities.forEach { entity ->
                    FilterChip(
                        label = entity.name,
                        isSelected = selectedEntityFilter == entity.id,
                        onClick = { selectedEntityFilter = entity.id }
                    )
                }
            }

            Divider(color = Color(0xFF333333))

            // Render Income Streams Section
            if (streamFilter == CashflowFilterType.ALL || streamFilter == CashflowFilterType.INCOME_ONLY) {
                val matchingIncome = household.incomeStreams.filter {
                    selectedEntityFilter == null || it.entityId == selectedEntityFilter
                }

                if (matchingIncome.isNotEmpty()) {
                    Text("Income Streams (${matchingIncome.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                    matchingIncome.forEach { stream ->
                        val index = household.incomeStreams.indexOfFirst { it.id == stream.id }
                        if (index != -1) {
                            IncomeStreamCard(
                                stream = stream,
                                household = household,
                                onUpdate = { updatedStream ->
                                    val updatedList = household.incomeStreams.toMutableList()
                                    updatedList[index] = updatedStream
                                    onUpdateHousehold(household.copy(incomeStreams = updatedList))
                                },
                                onDelete = {
                                    val updatedList = household.incomeStreams.filterIndexed { i, _ -> i != index }
                                    onUpdateHousehold(household.copy(incomeStreams = updatedList))
                                }
                            )
                        }
                    }
                }
            }

            // Render Asset Pools / Compounding Investment Section
            if (streamFilter == CashflowFilterType.ALL || streamFilter == CashflowFilterType.POOLS_ONLY) {
                val matchingPools = household.assetPools.filter {
                    selectedEntityFilter == null || it.entityId == selectedEntityFilter
                }

                if (matchingPools.isNotEmpty()) {
                    Text("Asset Pools / Compounding Accounts (${matchingPools.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5))
                    matchingPools.forEach { pool ->
                        val index = household.assetPools.indexOfFirst { it.id == pool.id }
                        if (index != -1) {
                            EditableAssetPoolCard(
                                pool = pool,
                                household = household,
                                onUpdate = { updatedPool ->
                                    val updatedList = household.assetPools.toMutableList()
                                    updatedList[index] = updatedPool
                                    onUpdateHousehold(household.copy(assetPools = updatedList))
                                },
                                onDelete = {
                                    val updatedList = household.assetPools.filterIndexed { i, _ -> i != index }
                                    onUpdateHousehold(household.copy(assetPools = updatedList))
                                }
                            )
                        }
                    }
                }
            }

            // Render Expenses & Debt Section
            if (streamFilter == CashflowFilterType.ALL || streamFilter == CashflowFilterType.EXPENSES_ONLY) {
                val matchingExpenses = household.expenses.filter {
                    selectedEntityFilter == null || it.entityId == selectedEntityFilter
                }

                if (matchingExpenses.isNotEmpty()) {
                    Text("Expenses & Compounding Debt (${matchingExpenses.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                    matchingExpenses.forEach { expense ->
                        val index = household.expenses.indexOfFirst { it.id == expense.id }
                        if (index != -1) {
                            EditableExpenseCard(
                                expense = expense,
                                household = household,
                                onUpdate = { updatedExpense ->
                                    val updatedList = household.expenses.toMutableList()
                                    updatedList[index] = updatedExpense
                                    onUpdateHousehold(household.copy(expenses = updatedList))
                                },
                                onDelete = {
                                    val updatedList = household.expenses.filterIndexed { i, _ -> i != index }
                                    onUpdateHousehold(household.copy(expenses = updatedList))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Income Stream Card with Multi-Phase Schedule Support
 */
@Composable
private fun IncomeStreamCard(
    stream: IncomeStream,
    household: Household,
    onUpdate: (IncomeStream) -> Unit,
    onDelete: () -> Unit
) {
    val associatedEntity = household.findEntity(stream.entityId) ?: household.primaryEntity()

    var name by remember(stream.id) { mutableStateOf(stream.name) }
    var startingPayText by remember(stream.id) { mutableStateOf((stream.initialAnnualAmount.value / 100).toString()) }
    var payBumpText by remember(stream.id) { mutableStateOf(((stream.yearlyPayBumpRate * 1000).toInt() / 10.0).toString()) }

    var startAgeText by remember(stream.id, stream.timeMode) { mutableStateOf(stream.startAge.toString()) }
    var endAgeText by remember(stream.id, stream.timeMode) { mutableStateOf(stream.endAge.toString()) }
    var startYearText by remember(stream.id, stream.timeMode) { mutableStateOf(stream.startYear.toString()) }
    var endYearText by remember(stream.id, stream.timeMode) { mutableStateOf(stream.endYear.toString()) }

    var entityMenuExpanded by remember { mutableStateOf(false) }
    var showPhases by remember { mutableStateOf(stream.phases.isNotEmpty()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF262626), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Name, Associated Entity, TimeMode Selector, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdate(stream.copy(name = it))
                    },
                    label = { Text("Income Stream Name") },
                    modifier = Modifier.weight(1.3f)
                )
                Spacer(Modifier.width(10.dp))

                // Entity Dropdown
                Box {
                    OutlinedButton(onClick = { entityMenuExpanded = true }) {
                        Text("Entity: ${associatedEntity.name}", fontSize = 11.sp, color = MaterialTheme.colors.primary)
                    }
                    DropdownMenu(
                        expanded = entityMenuExpanded,
                        onDismissRequest = { entityMenuExpanded = false }
                    ) {
                        household.entities.forEach { entity ->
                            DropdownMenuItem(onClick = {
                                entityMenuExpanded = false
                                onUpdate(stream.copy(entityId = entity.id))
                            }) {
                                Text("${entity.name} (Born ${entity.birthYear})")
                            }
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Timing Mode Toggle
                TimingModeToggle(
                    selectedMode = stream.timeMode,
                    onSelectMode = { newMode ->
                        if (newMode == TimeMode.CALENDAR_YEAR) {
                            val sYr = associatedEntity.yearAtAge(stream.startAge)
                            val eYr = associatedEntity.yearAtAge(stream.endAge)
                            onUpdate(stream.copy(timeMode = newMode, startYear = sYr, endYear = eYr))
                        } else {
                            val sAge = associatedEntity.ageInYear(stream.startYear)
                            val eAge = associatedEntity.ageInYear(stream.endYear)
                            onUpdate(stream.copy(timeMode = newMode, startAge = sAge, endAge = eAge))
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                ) {
                    Text("Delete", fontSize = 11.sp)
                }
            }

            // Row 2: Starting Pay, Start & End, Pay Bump %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = startingPayText,
                    onValueChange = {
                        startingPayText = it
                        val dollars = it.toLongOrNull() ?: 0L
                        onUpdate(stream.copy(initialAnnualAmount = Money.ofDollars(dollars)))
                    },
                    label = { Text("Starting Annual Pay ($)") },
                    modifier = Modifier.weight(1.2f)
                )

                if (stream.timeMode == TimeMode.ENTITY_AGE) {
                    OutlinedTextField(
                        value = startAgeText,
                        onValueChange = {
                            startAgeText = it
                            val age = it.toIntOrNull() ?: stream.startAge
                            val yr = associatedEntity.yearAtAge(age)
                            onUpdate(stream.copy(startAge = age, startYear = yr))
                        },
                        label = { Text("Start Age (${associatedEntity.name})") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = endAgeText,
                        onValueChange = {
                            endAgeText = it
                            val age = it.toIntOrNull() ?: stream.endAge
                            val yr = associatedEntity.yearAtAge(age)
                            onUpdate(stream.copy(endAge = age, endYear = yr))
                        },
                        label = { Text("Stop Age") },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    OutlinedTextField(
                        value = startYearText,
                        onValueChange = {
                            startYearText = it
                            val yr = it.toIntOrNull() ?: stream.startYear
                            val age = associatedEntity.ageInYear(yr)
                            onUpdate(stream.copy(startYear = yr, startAge = age))
                        },
                        label = { Text("Start Year") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = endYearText,
                        onValueChange = {
                            endYearText = it
                            val yr = it.toIntOrNull() ?: stream.endYear
                            val age = associatedEntity.ageInYear(yr)
                            onUpdate(stream.copy(endYear = yr, endAge = age))
                        },
                        label = { Text("Stop Year") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = payBumpText,
                    onValueChange = {
                        payBumpText = it
                        val bump = (it.toDoubleOrNull() ?: 0.0) / 100.0
                        onUpdate(stream.copy(yearlyPayBumpRate = bump))
                    },
                    label = { Text("Yearly Pay Bump (%)") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Multi-Phase Year-by-Year Schedules Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showPhases = !showPhases },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6))
                ) {
                    Text(if (showPhases) "Hide Year Schedules (${stream.phases.size})" else "+ Multi-Year Adjustments / Schedules (${stream.phases.size})", fontSize = 11.sp)
                }

                val effStartYear = stream.effectiveStartYear(associatedEntity)
                val effEndYear = stream.effectiveEndYear(associatedEntity)
                val effStartAge = associatedEntity.ageInYear(effStartYear)
                val effEndAge = associatedEntity.ageInYear(effEndYear)
                Text(
                    "Active: Years $effStartYear–$effEndYear (${associatedEntity.name} Ages $effStartAge–$effEndAge)",
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA)
                )
            }

            if (showPhases) {
                PhaseScheduleEditor(
                    phases = stream.phases,
                    entity = associatedEntity,
                    baseYear = household.baseYear,
                    onUpdatePhases = { onUpdate(stream.copy(phases = it)) }
                )
            }

            val trajectoryPoints = remember(stream, associatedEntity, household.baseYear) {
                val startYr = household.baseYear
                val endYr = household.baseYear + 40
                (startYr..endYr).map { yr ->
                    val age = associatedEntity.ageInYear(yr)
                    val inAmt = stream.amountInYear(yr, household.baseYear, associatedEntity, inflationRate = 0.025)
                    YearTrajectoryPoint(
                        calendarYear = yr,
                        age = age,
                        balance = Money.ZERO,
                        inflow = inAmt,
                        outflow = Money.ZERO
                    )
                }
            }

            EntryTrajectoryChart(
                title = "${stream.name} - Annual Cashflow Projection (Bar)",
                points = trajectoryPoints,
                chartMode = EntryChartMode.INCOME_STREAM,
                accentColor = Color(0xFF81C784)
            )
        }
    }
}

/**
 * Editable Asset Pool Card with Multi-Phase Year Schedules
 * (Asset pools compound on last year's balance + growth + contributions/withdrawals)
 */
@Composable
private fun EditableAssetPoolCard(
    pool: AssetPool,
    household: Household,
    onUpdate: (AssetPool) -> Unit,
    onDelete: () -> Unit
) {
    val associatedEntity = household.findEntity(pool.entityId) ?: household.primaryEntity()

    var name by remember(pool.id) { mutableStateOf(pool.name) }
    var balanceText by remember(pool.id) { mutableStateOf((pool.currentBalance.value / 100).toString()) }
    var returnRateText by remember(pool.id) { mutableStateOf(((pool.expectedNominalReturn * 1000).toInt() / 10.0).toString()) }
    var contributionText by remember(pool.id) { mutableStateOf((pool.annualContribution.value / 100).toString()) }
    var withdrawalText by remember(pool.id) { mutableStateOf((pool.annualWithdrawal.value / 100).toString()) }

    var entityMenuExpanded by remember { mutableStateOf(false) }
    var catMenuExpanded by remember { mutableStateOf(false) }
    var showPhases by remember { mutableStateOf(pool.phases.isNotEmpty()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF262626), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(Color(pool.category.hexColor), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            onUpdate(pool.copy(name = it))
                        },
                        label = { Text("Asset Pool Name") },
                        modifier = Modifier.width(200.dp)
                    )
                }

                // Owner Entity Dropdown
                Box {
                    OutlinedButton(onClick = { entityMenuExpanded = true }) {
                        Text("Owner: ${associatedEntity.name}", fontSize = 11.sp, color = MaterialTheme.colors.primary)
                    }
                    DropdownMenu(
                        expanded = entityMenuExpanded,
                        onDismissRequest = { entityMenuExpanded = false }
                    ) {
                        household.entities.forEach { entity ->
                            DropdownMenuItem(onClick = {
                                entityMenuExpanded = false
                                onUpdate(pool.copy(entityId = entity.id))
                            }) {
                                Text("${entity.name} (Born ${entity.birthYear})")
                            }
                        }
                    }
                }

                // Category Dropdown
                Box {
                    OutlinedButton(onClick = { catMenuExpanded = true }) {
                        Text(pool.category.displayName, fontSize = 11.sp, color = Color(pool.category.hexColor))
                    }
                    DropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false }
                    ) {
                        AssetCategory.entries.forEach { cat ->
                            DropdownMenuItem(onClick = {
                                catMenuExpanded = false
                                onUpdate(pool.copy(category = cat, expectedNominalReturn = cat.defaultReturnRate))
                            }) {
                                Text(cat.displayName, color = Color(cat.hexColor))
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                ) {
                    Text("Delete", fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = {
                        balanceText = it
                        val dollars = it.toLongOrNull() ?: 0L
                        onUpdate(pool.copy(currentBalance = Money.ofDollars(dollars)))
                    },
                    label = { Text("Starting Balance ($)") },
                    modifier = Modifier.weight(1.2f)
                )

                OutlinedTextField(
                    value = returnRateText,
                    onValueChange = {
                        returnRateText = it
                        val rate = (it.toDoubleOrNull() ?: 0.0) / 100.0
                        onUpdate(pool.copy(expectedNominalReturn = rate))
                    },
                    label = { Text("Expected Return (%)") },
                    modifier = Modifier.weight(1.1f)
                )

                OutlinedTextField(
                    value = contributionText,
                    onValueChange = {
                        contributionText = it
                        val dollars = it.toLongOrNull() ?: 0L
                        onUpdate(pool.copy(annualContribution = Money.ofDollars(dollars)))
                    },
                    label = { Text("Deposit/Contribution ($)") },
                    modifier = Modifier.weight(1.2f)
                )

                OutlinedTextField(
                    value = withdrawalText,
                    onValueChange = {
                        withdrawalText = it
                        val dollars = it.toLongOrNull() ?: 0L
                        onUpdate(pool.copy(annualWithdrawal = Money.ofDollars(dollars)))
                    },
                    label = { Text("Retirement Drawdown ($)") },
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Phase Schedule Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showPhases = !showPhases },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6))
                ) {
                    Text(if (showPhases) "Hide Year Schedules (${pool.phases.size})" else "+ Custom Contribution/Withdrawal Year Schedules (${pool.phases.size})", fontSize = 11.sp)
                }

                if (pool.phases.isNotEmpty()) {
                    Text("Active: ${pool.phases.size} customized year tiers (Deposits & Withdrawals)", fontSize = 11.sp, color = Color(0xFF81C784), fontWeight = FontWeight.SemiBold)
                }
            }

            if (showPhases) {
                PhaseScheduleEditor(
                    phases = pool.phases,
                    entity = associatedEntity,
                    baseYear = household.baseYear,
                    isAssetPool = true,
                    onUpdatePhases = { onUpdate(pool.copy(phases = it)) }
                )
            }

            val trajectoryPoints = remember(pool, associatedEntity, household.baseYear) {
                val startYr = household.baseYear
                val endYr = household.baseYear + 40
                var runningBalance = pool.currentBalance.value
                val realReturn = (1.0 + pool.expectedNominalReturn) / (1.0 + 0.025) - 1.0

                (startYr..endYr).map { yr ->
                    val age = associatedEntity.ageInYear(yr)
                    val contrib = pool.targetContributionInYear(yr, associatedEntity)
                    val withdr = pool.targetWithdrawalInYear(yr, associatedEntity)
                    val growth = if (runningBalance > 0L) (runningBalance * realReturn).toLong() else 0L

                    val netBeforeWithdrawal = runningBalance + growth + contrib.value
                    val isDeficit = withdr.value > 0L && netBeforeWithdrawal < withdr.value
                    val shortfallCents = if (isDeficit) withdr.value - netBeforeWithdrawal else 0L

                    runningBalance = max(0L, netBeforeWithdrawal - withdr.value)

                    YearTrajectoryPoint(
                        calendarYear = yr,
                        age = age,
                        balance = Money(runningBalance),
                        inflow = contrib,
                        outflow = withdr,
                        isDeficit = isDeficit || (runningBalance == 0L && withdr.value > 0L),
                        shortfall = Money(shortfallCents)
                    )
                }
            }

            EntryTrajectoryChart(
                title = "${pool.name} - Total Asset Balance, Inflow (Green) & Withdrawals (Red)",
                points = trajectoryPoints,
                chartMode = EntryChartMode.ASSET_POOL,
                accentColor = Color(pool.category.hexColor)
            )
        }
    }
}

/**
 * Editable Expense Card with Multi-Phase Year Schedules
 */
@Composable
private fun EditableExpenseCard(
    expense: ExpenseItem,
    household: Household,
    onUpdate: (ExpenseItem) -> Unit,
    onDelete: () -> Unit
) {
    val associatedEntity = household.findEntity(expense.entityId) ?: household.primaryEntity()

    var name by remember(expense.id) { mutableStateOf(expense.name) }
    var amountText by remember(expense.id) { mutableStateOf((expense.annualAmount.value / 100).toString()) }
    var interestText by remember(expense.id) { mutableStateOf(((expense.compoundingInterestRate * 1000).toInt() / 10.0).toString()) }

    var startAgeText by remember(expense.id, expense.timeMode) { mutableStateOf(expense.startAge.toString()) }
    var endAgeText by remember(expense.id, expense.timeMode) { mutableStateOf(expense.endAge.toString()) }
    var startYearText by remember(expense.id, expense.timeMode) { mutableStateOf(expense.startYear.toString()) }
    var endYearText by remember(expense.id, expense.timeMode) { mutableStateOf(expense.endYear.toString()) }

    var typeMenuExpanded by remember { mutableStateOf(false) }
    var catMenuExpanded by remember { mutableStateOf(false) }
    var entityMenuExpanded by remember { mutableStateOf(false) }
    var showPhases by remember { mutableStateOf(expense.phases.isNotEmpty()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF262626), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Name, Associated Entity, Timing Mode, Type, Category, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdate(expense.copy(name = it))
                    },
                    label = { Text("Expense Name") },
                    modifier = Modifier.weight(1.3f)
                )

                Spacer(Modifier.width(8.dp))

                // Entity Dropdown
                Box {
                    OutlinedButton(onClick = { entityMenuExpanded = true }) {
                        Text(associatedEntity.name, fontSize = 11.sp, color = MaterialTheme.colors.primary)
                    }
                    DropdownMenu(
                        expanded = entityMenuExpanded,
                        onDismissRequest = { entityMenuExpanded = false }
                    ) {
                        household.entities.forEach { entity ->
                            DropdownMenuItem(onClick = {
                                entityMenuExpanded = false
                                onUpdate(expense.copy(entityId = entity.id))
                            }) {
                                Text("${entity.name} (Born ${entity.birthYear})")
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Timing Mode Toggle
                TimingModeToggle(
                    selectedMode = expense.timeMode,
                    onSelectMode = { newMode ->
                        if (newMode == TimeMode.CALENDAR_YEAR) {
                            val sYr = associatedEntity.yearAtAge(expense.startAge)
                            val eYr = associatedEntity.yearAtAge(expense.endAge)
                            onUpdate(expense.copy(timeMode = newMode, startYear = sYr, endYear = eYr))
                        } else {
                            val sAge = associatedEntity.ageInYear(expense.startYear)
                            val eAge = associatedEntity.ageInYear(expense.endYear)
                            onUpdate(expense.copy(timeMode = newMode, startAge = sAge, endAge = eAge))
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                // Expense Type Dropdown
                Box {
                    OutlinedButton(onClick = { typeMenuExpanded = true }) {
                        Text(expense.expenseType.displayName, fontSize = 11.sp, color = MaterialTheme.colors.primary)
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        ExpenseType.entries.forEach { type ->
                            DropdownMenuItem(onClick = {
                                typeMenuExpanded = false
                                val newEndAge = if (type == ExpenseType.ONE_TIME) expense.startAge else expense.endAge
                                val newEndYear = if (type == ExpenseType.ONE_TIME) expense.startYear else expense.endYear
                                onUpdate(expense.copy(expenseType = type, endAge = newEndAge, endYear = newEndYear))
                            }) {
                                Text(type.displayName)
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Category Dropdown
                Box {
                    OutlinedButton(onClick = { catMenuExpanded = true }) {
                        Text(expense.category.displayName, fontSize = 11.sp, color = Color(expense.category.hexColor))
                    }
                    DropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false }
                    ) {
                        ExpenseCategory.entries.forEach { cat ->
                            DropdownMenuItem(onClick = {
                                catMenuExpanded = false
                                onUpdate(expense.copy(category = cat))
                            }) {
                                Text(cat.displayName, color = Color(cat.hexColor))
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                ) {
                    Text("Delete", fontSize = 11.sp)
                }
            }

            // Row 2: Amount, Start & Stop (Age or Year), Interest Rate (if compounding debt)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val amountLabel = when (expense.expenseType) {
                    ExpenseType.RECURRING -> "Annual Amount ($)"
                    ExpenseType.ONE_TIME -> "Lump Sum Amount ($)"
                    ExpenseType.COMPOUNDING_DEBT -> "Annual Payment ($)"
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        val dollars = it.toLongOrNull() ?: 0L
                        onUpdate(expense.copy(annualAmount = Money.ofDollars(dollars)))
                    },
                    label = { Text(amountLabel) },
                    modifier = Modifier.weight(1.2f)
                )

                if (expense.timeMode == TimeMode.ENTITY_AGE) {
                    OutlinedTextField(
                        value = startAgeText,
                        onValueChange = {
                            startAgeText = it
                            val age = it.toIntOrNull() ?: expense.startAge
                            val end = if (expense.expenseType == ExpenseType.ONE_TIME) age else expense.endAge
                            val sYr = associatedEntity.yearAtAge(age)
                            val eYr = associatedEntity.yearAtAge(end)
                            onUpdate(expense.copy(startAge = age, endAge = end, startYear = sYr, endYear = eYr))
                        },
                        label = { Text("Start Age (${associatedEntity.name})") },
                        modifier = Modifier.weight(1f)
                    )

                    if (expense.expenseType != ExpenseType.ONE_TIME) {
                        OutlinedTextField(
                            value = endAgeText,
                            onValueChange = {
                                endAgeText = it
                                val age = it.toIntOrNull() ?: expense.endAge
                                val eYr = associatedEntity.yearAtAge(age)
                                onUpdate(expense.copy(endAge = age, endYear = eYr))
                            },
                            label = { Text("Stop Age") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = startYearText,
                        onValueChange = {
                            startYearText = it
                            val yr = it.toIntOrNull() ?: expense.startYear
                            val end = if (expense.expenseType == ExpenseType.ONE_TIME) yr else expense.endYear
                            val sAge = associatedEntity.ageInYear(yr)
                            val eAge = associatedEntity.ageInYear(end)
                            onUpdate(expense.copy(startYear = yr, endYear = end, startAge = sAge, endAge = eAge))
                        },
                        label = { Text("Start Year") },
                        modifier = Modifier.weight(1f)
                    )

                    if (expense.expenseType != ExpenseType.ONE_TIME) {
                        OutlinedTextField(
                            value = endYearText,
                            onValueChange = {
                                endYearText = it
                                val yr = it.toIntOrNull() ?: expense.endYear
                                val eAge = associatedEntity.ageInYear(yr)
                                onUpdate(expense.copy(endYear = yr, endAge = eAge))
                            },
                            label = { Text("Stop Year") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (expense.expenseType == ExpenseType.COMPOUNDING_DEBT) {
                    OutlinedTextField(
                        value = interestText,
                        onValueChange = {
                            interestText = it
                            val rate = (it.toDoubleOrNull() ?: 0.0) / 100.0
                            onUpdate(expense.copy(compoundingInterestRate = rate))
                        },
                        label = { Text("Interest APR (%)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Phase Schedule Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showPhases = !showPhases },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6))
                ) {
                    Text(if (showPhases) "Hide Year Schedules (${expense.phases.size})" else "+ Custom Expense Year Schedules (${expense.phases.size})", fontSize = 11.sp)
                }

                val effStartYear = expense.effectiveStartYear(associatedEntity)
                val effEndYear = expense.effectiveEndYear(associatedEntity)
                val effStartAge = associatedEntity.ageInYear(effStartYear)
                val effEndAge = associatedEntity.ageInYear(effEndYear)
                Text(
                    "Active: Years $effStartYear–$effEndYear (${associatedEntity.name} Ages $effStartAge–$effEndAge)",
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA)
                )
            }

            if (showPhases) {
                PhaseScheduleEditor(
                    phases = expense.phases,
                    entity = associatedEntity,
                    baseYear = household.baseYear,
                    onUpdatePhases = { onUpdate(expense.copy(phases = it)) }
                )
            }

            val isLoanDebt = expense.expenseType == ExpenseType.COMPOUNDING_DEBT
            val trajectoryPoints = remember(expense, associatedEntity, household.baseYear) {
                val startYr = household.baseYear
                val endYr = household.baseYear + 40

                if (isLoanDebt) {
                    val effStart = expense.effectiveStartYear(associatedEntity)
                    val effEnd = expense.effectiveEndYear(associatedEntity)
                    val totalYears = max(1, effEnd - effStart + 1)
                    val annualPmt = expense.annualAmount.value
                    var remainingDebt = annualPmt * totalYears.toLong()

                    (startYr..endYr).map { yr ->
                        val age = associatedEntity.ageInYear(yr)
                        val pmt = expense.amountInYear(yr, household.baseYear, associatedEntity, inflationRate = 0.025)
                        if (yr in effStart..effEnd && remainingDebt > 0L) {
                            val interest = (remainingDebt * expense.compoundingInterestRate).toLong()
                            remainingDebt = max(0L, remainingDebt + interest - pmt.value)
                        } else if (yr > effEnd) {
                            remainingDebt = 0L
                        }
                        YearTrajectoryPoint(
                            calendarYear = yr,
                            age = age,
                            balance = Money(remainingDebt),
                            inflow = Money.ZERO,
                            outflow = pmt
                        )
                    }
                } else {
                    (startYr..endYr).map { yr ->
                        val age = associatedEntity.ageInYear(yr)
                        val outAmt = expense.amountInYear(yr, household.baseYear, associatedEntity, inflationRate = 0.025)
                        YearTrajectoryPoint(
                            calendarYear = yr,
                            age = age,
                            balance = Money.ZERO,
                            inflow = Money.ZERO,
                            outflow = outAmt
                        )
                    }
                }
            }

            EntryTrajectoryChart(
                title = if (isLoanDebt) "${expense.name} - Outstanding Debt Balance & Payments (Inverse)" else "${expense.name} - Annual Outflow (Bar)",
                points = trajectoryPoints,
                chartMode = if (isLoanDebt) EntryChartMode.COMPOUNDING_DEBT else EntryChartMode.EXPENSE_STREAM,
                accentColor = if (isLoanDebt) Color(0xFFEF5350) else Color(expense.category.hexColor)
            )
        }
    }
}

/**
 * Multi-Phase Schedule Editor (Reusable for Pools, Income Streams, and Expenses)
 */
@Composable
private fun PhaseScheduleEditor(
    phases: List<SchedulePhase>,
    entity: Entity,
    baseYear: Int,
    isAssetPool: Boolean = false,
    onUpdatePhases: (List<SchedulePhase>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Year-to-Year Schedules & Tiers", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)

            Button(
                onClick = {
                    val newPhase = SchedulePhase(
                        id = "phase_${phases.size + 1}",
                        name = "Tier #${phases.size + 1}",
                        timeMode = TimeMode.ENTITY_AGE,
                        startAge = entity.ageInYear(baseYear),
                        endAge = entity.retirementAge,
                        startYear = baseYear,
                        endYear = entity.yearAtAge(entity.retirementAge),
                        amount = Money.ofDollars(5_000),
                        isWithdrawal = false
                    )
                    onUpdatePhases(phases + newPhase)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF64B5F6))
            ) {
                Text("+ Add Year Tier", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        phases.forEachIndexed { idx, phase ->
            var phaseName by remember(phase.id) { mutableStateOf(phase.name) }
            var startAgeText by remember(phase.id) { mutableStateOf(phase.startAge.toString()) }
            var endAgeText by remember(phase.id) { mutableStateOf(phase.endAge.toString()) }
            var amountText by remember(phase.id) { mutableStateOf((phase.amount.value / 100).toString()) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF282828), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = phaseName,
                    onValueChange = {
                        phaseName = it
                        val updated = phases.toMutableList()
                        updated[idx] = phase.copy(name = it)
                        onUpdatePhases(updated)
                    },
                    label = { Text("Tier Label") },
                    modifier = Modifier.weight(1.2f)
                )

                if (isAssetPool) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (phase.isWithdrawal) Color(0xFFEF5350).copy(alpha = 0.25f) else Color(0xFF81C784).copy(alpha = 0.25f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (phase.isWithdrawal) Color(0xFFEF5350) else Color(0xFF81C784),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                val updated = phases.toMutableList()
                                updated[idx] = phase.copy(isWithdrawal = !phase.isWithdrawal)
                                onUpdatePhases(updated)
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (phase.isWithdrawal) "Withdrawal (Red)" else "Deposit (Green)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (phase.isWithdrawal) Color(0xFFEF5350) else Color(0xFF81C784)
                        )
                    }
                }

                OutlinedTextField(
                    value = startAgeText,
                    onValueChange = {
                        startAgeText = it
                        val age = it.toIntOrNull() ?: phase.startAge
                        val updated = phases.toMutableList()
                        updated[idx] = phase.copy(startAge = age, startYear = entity.yearAtAge(age))
                        onUpdatePhases(updated)
                    },
                    label = { Text("Start Age") },
                    modifier = Modifier.weight(0.8f)
                )

                OutlinedTextField(
                    value = endAgeText,
                    onValueChange = {
                        endAgeText = it
                        val age = it.toIntOrNull() ?: phase.endAge
                        val updated = phases.toMutableList()
                        updated[idx] = phase.copy(endAge = age, endYear = entity.yearAtAge(age))
                        onUpdatePhases(updated)
                    },
                    label = { Text("End Age") },
                    modifier = Modifier.weight(0.8f)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        val dollars = it.toLongOrNull() ?: 0L
                        val updated = phases.toMutableList()
                        updated[idx] = phase.copy(amount = Money.ofDollars(dollars))
                        onUpdatePhases(updated)
                    },
                    label = { Text(if (phase.isWithdrawal) "Withdrawal ($)" else "Annual ($)") },
                    modifier = Modifier.weight(1.2f)
                )

                OutlinedButton(
                    onClick = {
                        val updated = phases.filterIndexed { i, _ -> i != idx }
                        onUpdatePhases(updated)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                ) {
                    Text("✕", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun TimingModeToggle(
    selectedMode: TimeMode,
    onSelectMode: (TimeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(6.dp))
            .padding(2.dp)
    ) {
        TimeMode.entries.forEach { mode ->
            val isSelected = selectedMode == mode
            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.25f) else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    mode.displayName,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colors.primary else Color(0xFFAAAAAA)
                )
            }
        }
    }
}

/**
 * Cashflow & Preferential Payment Priority Manager Tab
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CashflowPriorityManager(
    household: Household,
    timeline: List<YearCategoryBreakdown>,
    onUpdateHousehold: (Household) -> Unit
) {
    val activeRules = remember(household) { household.activePriorityRules() }

    var selectedEntityFilter by remember { mutableStateOf<String?>(null) }
    var selectedTargetId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Section 1: Preferential Payment & Investment Priority Order
        Card(
            backgroundColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Preferential Payment & Investment Order (Waterfall)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Annual income is routed strictly in this order. Move items up or down to adjust which pools/debts are funded first.",
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onUpdateHousehold(household.copy(priorityRules = household.defaultPriorityRules()))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6))
                    ) {
                        Text("Reset to Recommended Order", fontSize = 11.sp)
                    }
                }

                Divider(color = Color(0xFF333333))

                activeRules.forEachIndexed { index, rule ->
                    PriorityRuleCard(
                        rule = rule,
                        household = household,
                        onMoveUp = if (index > 0) {
                            {
                                val reordered = activeRules.toMutableList()
                                val temp = reordered[index - 1]
                                reordered[index - 1] = rule.copy(priorityRank = index)
                                reordered[index] = temp.copy(priorityRank = index + 1)
                                onUpdateHousehold(household.copy(priorityRules = reordered))
                            }
                        } else null,
                        onMoveDown = if (index < activeRules.size - 1) {
                            {
                                val reordered = activeRules.toMutableList()
                                val temp = reordered[index + 1]
                                reordered[index + 1] = rule.copy(priorityRank = index + 2)
                                reordered[index] = temp.copy(priorityRank = index + 1)
                                onUpdateHousehold(household.copy(priorityRules = reordered))
                            }
                        } else null
                    )
                }
            }
        }

        // Section 2: Entity Investment & Payout Timeline (Highlights Unfunded / Missed segments in RED)
        Card(
            backgroundColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Text("Entity Investment & Payout Timeline", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Click on an entity or investment below to inspect year-by-year funding. Any segment where investments or payouts are starved/missed is highlighted in RED.",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }

                // Entity Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Entity:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFAAAAAA))

                    FilterChip(
                        label = "All Entities",
                        isSelected = selectedEntityFilter == null,
                        onClick = {
                            selectedEntityFilter = null
                            selectedTargetId = null
                        }
                    )

                    household.entities.forEach { entity ->
                        FilterChip(
                            label = entity.name,
                            isSelected = selectedEntityFilter == entity.id,
                            onClick = {
                                selectedEntityFilter = entity.id
                                selectedTargetId = null
                            }
                        )
                    }
                }

                // Item Filter Chips
                val relevantRules = activeRules.filter {
                    selectedEntityFilter == null || it.entityId == selectedEntityFilter
                }

                if (relevantRules.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            label = "All Items",
                            isSelected = selectedTargetId == null,
                            onClick = { selectedTargetId = null }
                        )

                        relevantRules.forEach { r ->
                            FilterChip(
                                label = r.name,
                                isSelected = selectedTargetId == r.targetId,
                                onClick = { selectedTargetId = r.targetId }
                            )
                        }
                    }
                }

                Divider(color = Color(0xFF333333))

                // Timeline Render
                EntityTimelineGrid(
                    timeline = timeline,
                    selectedEntityId = selectedEntityFilter,
                    selectedTargetId = selectedTargetId,
                    household = household
                )
            }
        }
    }
}

@Composable
private fun PriorityRuleCard(
    rule: PriorityRule,
    household: Household,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    val associatedEntity = household.findEntity(rule.entityId)

    // Lookup target amount
    val targetAmountStr = when (rule.targetType) {
        PriorityTargetType.EXPENSE_PAYOUT -> {
            val exp = household.allExpenses().firstOrNull { it.id == rule.targetId }
            exp?.annualAmount?.toFormattedString() ?: "$0.00"
        }
        PriorityTargetType.POOL_CONTRIBUTION -> {
            val pool = household.allAssetPools().firstOrNull { it.id == rule.targetId }
            pool?.annualContribution?.toFormattedString() ?: "$0.00"
        }
        PriorityTargetType.SURPLUS_INVESTMENT -> "Surplus Balance"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF262626), RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (rule.enabled) Color(rule.itemType.hexColor).copy(alpha = 0.4f) else Color(0xFF3A3A3A),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rank Badge & Name
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(rule.itemType.hexColor).copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, Color(rule.itemType.hexColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${rule.priorityRank}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(rule.itemType.hexColor)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(rule.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        if (associatedEntity != null) {
                            Text("(${associatedEntity.name})", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                        }
                    }
                    Text(
                        "${rule.itemType.displayName} • Target: $targetAmountStr/yr",
                        fontSize = 11.sp,
                        color = Color(rule.itemType.hexColor)
                    )
                }
            }

            // Up / Down Reorder Buttons
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onMoveUp != null) {
                    OutlinedButton(onClick = onMoveUp, modifier = Modifier.size(36.dp, 32.dp)) {
                        Text("▲", fontSize = 11.sp)
                    }
                }
                if (onMoveDown != null) {
                    OutlinedButton(onClick = onMoveDown, modifier = Modifier.size(36.dp, 32.dp)) {
                        Text("▼", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Renders the Year-by-Year Entity Funding Timeline with RED highlighting for unfunded segments
 */
@Composable
private fun EntityTimelineGrid(
    timeline: List<YearCategoryBreakdown>,
    selectedEntityId: String?,
    selectedTargetId: String?,
    household: Household
) {
    val entityName = if (selectedEntityId != null) {
        household.findEntity(selectedEntityId)?.name ?: "Selected Entity"
    } else "Household Consolidated"

    // Calculate shortfall statistics
    var totalPlannedSegments = 0
    var fullyFundedCount = 0
    var problemCount = 0
    var totalShortfallCents = 0L

    timeline.forEach { yr ->
        val fundings = yr.itemFundings.filter { f ->
            (selectedEntityId == null || f.entityId == selectedEntityId) &&
            (selectedTargetId == null || f.id == selectedTargetId)
        }
        if (fundings.isNotEmpty()) {
            totalPlannedSegments++
            if (fundings.any { it.status.isProblem }) {
                problemCount++
                totalShortfallCents += fundings.sumOf { it.shortfall.value }
            } else {
                fullyFundedCount++
            }
        }
    }

    // Header Statistics Banner
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF242424), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Timeline for: $entityName", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colors.primary)
            Text("Active Target Segments: $totalPlannedSegments Years Evaluated", fontSize = 11.sp, color = Color(0xFFAAAAAA))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Fully Funded: $fullyFundedCount yrs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF81C784)
                )
                if (problemCount > 0) {
                    Text(
                        "⚠️ Unfunded/Short: $problemCount yrs (-${Money(totalShortfallCents).toFormattedString()})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFEF5350)
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    // List of Year Cards
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        timeline.forEach { yr ->
            val relevantFundings = yr.itemFundings.filter { f ->
                (selectedEntityId == null || f.entityId == selectedEntityId) &&
                (selectedTargetId == null || f.id == selectedTargetId)
            }

            if (relevantFundings.isNotEmpty()) {
                val hasProblem = relevantFundings.any { it.status.isProblem }

                // Card with RED background & border when unfunded/missed!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (hasProblem) Color(0xFF381515) else Color(0xFF222222),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (hasProblem) Color(0xFFEF5350) else Color(0xFF333333),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "${yr.calendarYear} (Age ${yr.age})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )

                                if (hasProblem) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFEF5350).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFFEF5350), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("⚠️ NOT INVESTED / UNPAID SHORTFALL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF81C784).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFF81C784), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("✓ 100% FUNDED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                                    }
                                }
                            }

                            Text(
                                "Total Income: ${yr.totalIncome.toFormattedString()}",
                                fontSize = 11.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        }

                        // Itemized target vs actual
                        relevantFundings.forEach { f ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).background(Color(f.itemType.hexColor), CircleShape))
                                    Text(f.name, fontSize = 12.sp, color = Color.White)
                                    Text("(${f.itemType.displayName})", fontSize = 10.sp, color = Color(0xFF888888))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Target: ${f.targetAmount.toFormattedString()}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFAAAAAA)
                                    )
                                    Text(
                                        "Invested/Paid: ${f.actualAmount.toFormattedString()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (f.status.isProblem) Color(0xFFEF5350) else Color(0xFF81C784)
                                    )
                                    if (f.status.isProblem) {
                                        Text(
                                            "Shortfall: -${f.shortfall.toFormattedString()}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF5350)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Entities and Households Management Section
 */
@Composable
private fun EntitiesAndHouseholdsManager(
    primaryHousehold: Household,
    elderCareHousehold: Household?,
    onUpdatePrimary: (Household) -> Unit,
    onUpdateElderCare: (Household?) -> Unit
) {
    Card(
        backgroundColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Entities & Birth Dates", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Manage individuals, children, and personas with their birth years. All income and expenses map cleanly to each person.",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }

                Button(
                    onClick = {
                        val newId = "entity_${primaryHousehold.entities.size + 1}"
                        val newEntity = Entity(
                            id = newId,
                            name = "Family Member #${primaryHousehold.entities.size + 1}",
                            birthYear = primaryHousehold.baseYear - 25,
                            isPrimary = false,
                            retirementAge = 65,
                            lifeExpectancy = 90
                        )
                        onUpdatePrimary(primaryHousehold.copy(entities = primaryHousehold.entities + newEntity))
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Text("+ Add Entity / Member", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Divider(color = Color(0xFF333333))

            Text("Primary Household Entities:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)

            primaryHousehold.entities.forEachIndexed { index, entity ->
                EditableEntityCard(
                    entity = entity,
                    baseYear = primaryHousehold.baseYear,
                    onUpdate = { updatedEntity ->
                        val updatedList = primaryHousehold.entities.toMutableList()
                        updatedList[index] = updatedEntity
                        onUpdatePrimary(primaryHousehold.copy(entities = updatedList))
                    },
                    onDelete = if (!entity.isPrimary) {
                        {
                            val updatedList = primaryHousehold.entities.filterIndexed { i, _ -> i != index }
                            onUpdatePrimary(primaryHousehold.copy(entities = updatedList))
                        }
                    } else null
                )
            }

            Divider(color = Color(0xFF333333))

            // Multi-Household Entities
            Text("Separate Financial Persona Entities:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))

            if (elderCareHousehold != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF262626), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Entity Persona: ${elderCareHousehold.name}", fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                            Spacer(Modifier.height(4.dp))
                            Text("Initial Net Worth: ${elderCareHousehold.totalInitialNetWorth().toFormattedString()} | Income: ${elderCareHousehold.incomeStreams.size} streams", fontSize = 12.sp, color = Color(0xFFCCCCCC))
                            Text("Entities: ${elderCareHousehold.entities.joinToString { "${it.name} (b. ${it.birthYear})" }}", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                        }
                        OutlinedButton(
                            onClick = { onUpdateElderCare(null) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                        ) {
                            Text("Remove Persona", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        onUpdateElderCare(
                            Household(
                                id = "elder_care",
                                name = "Aging Parents Support",
                                isPrimary = false,
                                baseYear = primaryHousehold.baseYear,
                                entities = listOf(
                                    Entity("entity_parent", "Robert (Parent)", birthYear = 1961, isPrimary = true, retirementAge = 65, lifeExpectancy = 90)
                                ),
                                incomeStreams = listOf(
                                    IncomeStream("inc_parents_ss", "Social Security & Pension", Money.ofDollars(28_000), entityId = "entity_parent", timeMode = TimeMode.CALENDAR_YEAR, startAge = 65, endAge = 90, startYear = 2026, endYear = 2051, yearlyPayBumpRate = 0.0)
                                ),
                                assetPools = listOf(
                                    AssetPool("ec_cash", "Parents Savings", AssetCategory.CASH_EMERGENCY, Money.ofDollars(50_000), entityId = "entity_parent", expectedNominalReturn = 0.035),
                                    AssetPool("ec_ira", "Parents Traditional IRA", AssetCategory.PRE_TAX_401K, Money.ofDollars(120_000), entityId = "entity_parent", expectedNominalReturn = 0.055)
                                ),
                                expenses = listOf(
                                    ExpenseItem("ec_care", "Assisted Living & Healthcare", ExpenseCategory.HEALTHCARE, Money.ofDollars(35_000), entityId = "entity_parent", timeMode = TimeMode.CALENDAR_YEAR, startAge = 65, endAge = 90, startYear = 2026, endYear = 2051, expenseType = ExpenseType.RECURRING)
                                )
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF81C784))
                ) {
                    Text("+ Add Aging Parents Persona", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditableEntityCard(
    entity: Entity,
    baseYear: Int,
    onUpdate: (Entity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(entity.id) { mutableStateOf(entity.name) }
    var birthYearText by remember(entity.id) { mutableStateOf(entity.birthYear.toString()) }
    var retirementAgeText by remember(entity.id) { mutableStateOf(entity.retirementAge.toString()) }
    var lifeExpectancyText by remember(entity.id) { mutableStateOf(entity.lifeExpectancy.toString()) }

    val currentAge = entity.ageInYear(baseYear)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF262626), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdate(entity.copy(name = it))
                    },
                    label = { Text("Person / Entity Name") },
                    modifier = Modifier.weight(1.5f)
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    "Current Age in $baseYear: Age $currentAge",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                if (onDelete != null) {
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373))
                    ) {
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = birthYearText,
                    onValueChange = {
                        birthYearText = it
                        val yr = it.toIntOrNull() ?: entity.birthYear
                        onUpdate(entity.copy(birthYear = yr))
                    },
                    label = { Text("Birth Year") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = retirementAgeText,
                    onValueChange = {
                        retirementAgeText = it
                        val age = it.toIntOrNull() ?: entity.retirementAge
                        onUpdate(entity.copy(retirementAge = age))
                    },
                    label = { Text("Retirement Age") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = lifeExpectancyText,
                    onValueChange = {
                        lifeExpectancyText = it
                        val age = it.toIntOrNull() ?: entity.lifeExpectancy
                        onUpdate(entity.copy(lifeExpectancy = age))
                    },
                    label = { Text("Life Expectancy") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
