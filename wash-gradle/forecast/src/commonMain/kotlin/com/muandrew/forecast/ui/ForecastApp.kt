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
import com.muandrew.forecast.engine.MultiAssetEngine
import com.muandrew.forecast.engine.YearCategoryBreakdown
import com.muandrew.forecast.model.AssetCategory
import com.muandrew.forecast.model.AssetPool
import com.muandrew.forecast.model.Entity
import com.muandrew.forecast.model.ExpenseCategory
import com.muandrew.forecast.model.ExpenseItem
import com.muandrew.forecast.model.ExpenseType
import com.muandrew.forecast.model.FinancialPlan
import com.muandrew.forecast.model.Household
import com.muandrew.forecast.model.IncomeStream
import com.muandrew.forecast.model.Money
import com.muandrew.forecast.model.TimeMode

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
    INCOME_STREAMS("Income Streams"),
    ASSET_POOLS("Asset Pools"),
    EXPENSES("Expenses"),
    ENTITIES("Entities & Households")
}

@Composable
fun ForecastApp() {
    MaterialTheme(colors = DarkColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            val basePlanYear = 2026

            // Initial default household state with entities, income streams, and expenses
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
                            AssetPool("p_taxable", "Taxable Brokerage", AssetCategory.TAXABLE_BROKERAGE, Money.ofDollars(50_000), entityId = "entity_primary", expectedNominalReturn = 0.075, annualContribution = Money.ofDollars(15_000)),
                            AssetPool("p_401k", "Workplace 401(k)", AssetCategory.PRE_TAX_401K, Money.ofDollars(40_000), entityId = "entity_primary", expectedNominalReturn = 0.070, annualContribution = Money.ofDollars(23_000)),
                            AssetPool("p_roth", "Roth IRA", AssetCategory.ROTH_IRA, Money.ofDollars(20_000), entityId = "entity_primary", expectedNominalReturn = 0.075, annualContribution = Money.ofDollars(7_000)),
                            AssetPool("p_cash", "Emergency HYSA", AssetCategory.CASH_EMERGENCY, Money.ofDollars(25_000), entityId = "entity_primary", expectedNominalReturn = 0.035, annualContribution = Money.ofDollars(2_000)),
                            AssetPool("p_529", "Emma's 529 College Fund", AssetCategory.TAXABLE_BROKERAGE, Money.ofDollars(10_000), entityId = "entity_child", expectedNominalReturn = 0.070, annualContribution = Money.ofDollars(6_000))
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
                            SummaryMetricsGrid(activeResult = activeResult)
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

                    AppTab.INCOME_STREAMS -> {
                        item {
                            IncomeStreamsManager(
                                household = primaryHousehold,
                                onUpdateHousehold = { primaryHousehold = it }
                            )
                        }
                    }

                    AppTab.ASSET_POOLS -> {
                        item {
                            AssetPoolsManager(
                                household = primaryHousehold,
                                onUpdateHousehold = { primaryHousehold = it }
                            )
                        }
                    }

                    AppTab.EXPENSES -> {
                        item {
                            ExpensesManager(
                                household = primaryHousehold,
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
            text = "Multi-Entity Forecast Engine with Birth Dates, Absolute Years & Entity Age Timings",
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
private fun SummaryMetricsGrid(activeResult: com.muandrew.forecast.engine.ConsolidatedPlanResult) {
    val finalNW = activeResult.finalNetWorth
    val p10 = activeResult.p10Path.lastOrNull() ?: Money.ZERO
    val p90 = activeResult.p90Path.lastOrNull() ?: Money.ZERO
    val success = activeResult.overallSuccessRate

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
            title = "Multi-Entity Timeline",
            value = "Active",
            subtitle = "Entity birth dates & age/year timings synced",
            color = Color(0xFFAB47BC),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        backgroundColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
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
 * Income Streams Management Section
 */
@Composable
private fun IncomeStreamsManager(
    household: Household,
    onUpdateHousehold: (Household) -> Unit
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
                    Text("Income Streams", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Link income to specific entities, with timing set by Entity Age or Absolute Calendar Year.",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }

                Button(
                    onClick = {
                        val primary = household.primaryEntity()
                        val newId = "income_${household.incomeStreams.size + 1}"
                        val newStream = IncomeStream(
                            id = newId,
                            name = "Additional Income",
                            entityId = primary.id,
                            initialAnnualAmount = Money.ofDollars(50_000),
                            timeMode = TimeMode.ENTITY_AGE,
                            startAge = primary.ageInYear(household.baseYear),
                            endAge = primary.retirementAge,
                            startYear = household.baseYear,
                            endYear = primary.yearAtAge(primary.retirementAge),
                            yearlyPayBumpRate = 0.03
                        )
                        onUpdateHousehold(household.copy(incomeStreams = household.incomeStreams + newStream))
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF81C784))
                ) {
                    Text("+ Add Income Stream", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            var selectedEntityFilter by remember { mutableStateOf<String?>(null) }

            // Entity Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Filter by Entity:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFAAAAAA))

                FilterChip(
                    label = "All Entities (${household.incomeStreams.size})",
                    isSelected = selectedEntityFilter == null,
                    onClick = { selectedEntityFilter = null }
                )

                household.entities.forEach { entity ->
                    val count = household.incomeStreams.count { it.entityId == entity.id }
                    FilterChip(
                        label = "${entity.name} ($count)",
                        isSelected = selectedEntityFilter == entity.id,
                        onClick = { selectedEntityFilter = entity.id }
                    )
                }
            }

            Divider(color = Color(0xFF333333))

            val displayedStreams = household.incomeStreams.filter {
                selectedEntityFilter == null || it.entityId == selectedEntityFilter
            }

            if (displayedStreams.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No income streams match the selected entity filter.", color = Color.Gray, fontSize = 13.sp)
                }
            }

            displayedStreams.forEach { stream ->
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
}

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

                // Timing Mode Toggle: Entity Age vs Calendar Year
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

            // Row 2: Starting Pay, Start & End (Age or Year), Pay Bump %
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

            // Timing Conversion Summary
            val effStartYear = stream.effectiveStartYear(associatedEntity)
            val effEndYear = stream.effectiveEndYear(associatedEntity)
            val effStartAge = associatedEntity.ageInYear(effStartYear)
            val effEndAge = associatedEntity.ageInYear(effEndYear)

            val startPay = stream.initialAnnualAmount
            val finalPay = stream.amountInYear(effEndYear, household.baseYear, associatedEntity, 0.0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Active: Years $effStartYear–$effEndYear (${associatedEntity.name} Ages $effStartAge–$effEndAge)",
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA)
                )
                Text(
                    "Starting: ${startPay.toFormattedString()}/yr → Peak: ${finalPay.toFormattedString()}/yr",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81C784)
                )
            }
        }
    }
}

/**
 * Asset Pools Management Section
 */
@Composable
private fun AssetPoolsManager(
    household: Household,
    onUpdateHousehold: (Household) -> Unit
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
                    Text("Asset Pools & Projected Growth Curves", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Edit starting amounts, expected nominal return rates, and annual contributions per asset category.",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }

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
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    Text("+ Add Asset Pool", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            var selectedEntityFilter by remember { mutableStateOf<String?>(null) }

            // Entity Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Filter by Owner:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFAAAAAA))

                FilterChip(
                    label = "All Owners (${household.assetPools.size})",
                    isSelected = selectedEntityFilter == null,
                    onClick = { selectedEntityFilter = null }
                )

                household.entities.forEach { entity ->
                    val count = household.assetPools.count { it.entityId == entity.id }
                    FilterChip(
                        label = "${entity.name} ($count)",
                        isSelected = selectedEntityFilter == entity.id,
                        onClick = { selectedEntityFilter = entity.id }
                    )
                }
            }

            Divider(color = Color(0xFF333333))

            val displayedPools = household.assetPools.filter {
                selectedEntityFilter == null || it.entityId == selectedEntityFilter
            }

            if (displayedPools.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No asset pools match the selected owner filter.", color = Color.Gray, fontSize = 13.sp)
                }
            }

            displayedPools.forEach { pool ->
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
}

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

    var entityMenuExpanded by remember { mutableStateOf(false) }

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
                        label = { Text("Pool Name") },
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

                Text(
                    "Category: ${pool.category.displayName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(pool.category.hexColor)
                )

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
                    label = { Text("Starting Amount ($)") },
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
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = contributionText,
                    onValueChange = {
                        contributionText = it
                        val dollars = it.toLongOrNull() ?: 0L
                        onUpdate(pool.copy(annualContribution = Money.ofDollars(dollars)))
                    },
                    label = { Text("Annual Contribution ($)") },
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

/**
 * Expenses Management Section with Associated Entities & Timing Modes
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpensesManager(
    household: Household,
    onUpdateHousehold: (Household) -> Unit
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
                    Text("Expenses & Outflows", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Associate expenses with specific entities or the household, with timing by Entity Age or Calendar Year.",
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
                Button(
                    onClick = {
                        val primary = household.primaryEntity()
                        val newId = "exp_rec_${household.expenses.size + 1}"
                        val newItem = ExpenseItem(
                            id = newId,
                            name = "New Recurring Expense",
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
                    Text("+ Add Recurring Expense", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val primary = household.primaryEntity()
                        val newId = "exp_onetime_${household.expenses.size + 1}"
                        val newItem = ExpenseItem(
                            id = newId,
                            name = "One-Time Milestone Purchase",
                            category = ExpenseCategory.MILESTONE_OTHER,
                            annualAmount = Money.ofDollars(30_000),
                            entityId = primary.id,
                            timeMode = TimeMode.CALENDAR_YEAR,
                            startAge = primary.ageInYear(household.baseYear + 2),
                            endAge = primary.ageInYear(household.baseYear + 2),
                            startYear = household.baseYear + 2,
                            endYear = household.baseYear + 2,
                            expenseType = ExpenseType.ONE_TIME
                        )
                        onUpdateHousehold(household.copy(expenses = household.expenses + newItem))
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFAB47BC))
                ) {
                    Text("+ Add One-Time Expense", color = Color.Black, fontWeight = FontWeight.Bold)
                }

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
                    Text("+ Add Compounding Debt", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val childEntity = household.entities.firstOrNull { it.id.contains("child") }
                            ?: household.entities.firstOrNull { !it.isPrimary }
                            ?: household.primaryEntity()
                        val childExpenses = listOf(
                            ExpenseItem("child_daycare_${household.expenses.size}", "${childEntity.name} - Daycare (Ages 0-5)", ExpenseCategory.CHILDCARE_EARLY, Money.ofDollars(18_000), entityId = childEntity.id, timeMode = TimeMode.ENTITY_AGE, startAge = 0, endAge = 5, startYear = childEntity.birthYear, endYear = childEntity.birthYear + 5, expenseType = ExpenseType.RECURRING),
                            ExpenseItem("child_living_${household.expenses.size}", "${childEntity.name} - School Living (Ages 6-17)", ExpenseCategory.LIVING_ESSENTIALS, Money.ofDollars(8_000), entityId = childEntity.id, timeMode = TimeMode.ENTITY_AGE, startAge = 6, endAge = 17, startYear = childEntity.birthYear + 6, endYear = childEntity.birthYear + 17, expenseType = ExpenseType.RECURRING),
                            ExpenseItem("child_529_${household.expenses.size}", "${childEntity.name} - 529 College Savings", ExpenseCategory.EDUCATION_TUITION, Money.ofDollars(6_000), entityId = childEntity.id, timeMode = TimeMode.ENTITY_AGE, startAge = 0, endAge = 17, startYear = childEntity.birthYear, endYear = childEntity.birthYear + 17, expenseType = ExpenseType.RECURRING),
                            ExpenseItem("child_college_${household.expenses.size}", "${childEntity.name} - College Tuition (Ages 18-21)", ExpenseCategory.EDUCATION_TUITION, Money.ofDollars(35_000), entityId = childEntity.id, timeMode = TimeMode.ENTITY_AGE, startAge = 18, endAge = 21, startYear = childEntity.birthYear + 18, endYear = childEntity.birthYear + 21, expenseType = ExpenseType.RECURRING)
                        )
                        onUpdateHousehold(household.copy(expenses = household.expenses + childExpenses))
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFA726))
                ) {
                    Text("+ Add Child Lifecycle (4 Stages)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            var selectedEntityFilter by remember { mutableStateOf<String?>(null) }

            // Entity Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Filter by Entity:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFAAAAAA))

                FilterChip(
                    label = "All Entities (${household.expenses.size})",
                    isSelected = selectedEntityFilter == null,
                    onClick = { selectedEntityFilter = null }
                )

                household.entities.forEach { entity ->
                    val count = household.expenses.count { it.entityId == entity.id }
                    FilterChip(
                        label = "${entity.name} ($count)",
                        isSelected = selectedEntityFilter == entity.id,
                        onClick = { selectedEntityFilter = entity.id }
                    )
                }
            }

            Divider(color = Color(0xFF333333))

            val displayedExpenses = household.expenses.filter {
                selectedEntityFilter == null || it.entityId == selectedEntityFilter
            }

            if (displayedExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No expenses match the selected entity filter.", color = Color.Gray, fontSize = 13.sp)
                }
            }

            displayedExpenses.forEach { expense ->
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

            // Duration and total projection badge
            val effStartYear = expense.effectiveStartYear(associatedEntity)
            val effEndYear = expense.effectiveEndYear(associatedEntity)
            val effStartAge = associatedEntity.ageInYear(effStartYear)
            val effEndAge = associatedEntity.ageInYear(effEndYear)

            val startVal = expense.amountInYear(effStartYear, household.baseYear, associatedEntity, 0.0)
            val endVal = expense.amountInYear(effEndYear, household.baseYear, associatedEntity, 0.0)
            val activeYears = (effEndYear - effStartYear + 1).coerceAtLeast(1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (expense.expenseType == ExpenseType.ONE_TIME) {
                    Text("One-Time Impact in Year $effStartYear (${associatedEntity.name} Age $effStartAge): ${startVal.toFormattedString()}", fontSize = 11.sp, color = Color(0xFFAB47BC), fontWeight = FontWeight.SemiBold)
                } else if (expense.expenseType == ExpenseType.COMPOUNDING_DEBT) {
                    Text("Compounding Debt: Years $effStartYear–$effEndYear (${associatedEntity.name} Ages $effStartAge–$effEndAge, $activeYears yrs)", fontSize = 11.sp, color = Color(0xFFEF5350))
                    Text("Final Payment: ${endVal.toFormattedString()}/yr at ${(expense.compoundingInterestRate * 100).toInt()}% APR", fontSize = 11.sp, color = Color(0xFFFFA726))
                } else {
                    Text("Active: Years $effStartYear–$effEndYear (${associatedEntity.name} Ages $effStartAge–$effEndAge, $activeYears yrs)", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    Text("Baseline: ${startVal.toFormattedString()}/yr", fontSize = 11.sp, color = Color(0xFF64B5F6))
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
