package com.muandrew.forecast.engine

import com.muandrew.forecast.model.AssetCategory
import com.muandrew.forecast.model.AssetPool
import com.muandrew.forecast.model.ExpenseCategory
import com.muandrew.forecast.model.FinancialPlan
import com.muandrew.forecast.model.FundingStatus
import com.muandrew.forecast.model.HomePurchaseTemplate
import com.muandrew.forecast.model.Household
import com.muandrew.forecast.model.Money
import com.muandrew.forecast.model.PriorityItemType
import com.muandrew.forecast.model.PriorityTargetType
import com.muandrew.forecast.model.YearlyItemFunding
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

data class YearCategoryBreakdown(
    val yearIndex: Int,
    val calendarYear: Int,
    val age: Int,
    val assetBalances: Map<AssetCategory, Money>,
    val expenseBalances: Map<ExpenseCategory, Money>,
    val totalNetWorth: Money,
    val totalIncome: Money,
    val totalExpenses: Money,
    val netCashFlow: Money,
    val itemFundings: List<YearlyItemFunding> = emptyList(),
    val unfundedCount: Int = itemFundings.count { it.status.isProblem },
    val poolBalances: Map<String, Money> = emptyMap(),
    val poolFlows: Map<String, PoolYearFlow> = emptyMap(),
    val totalLiabilities: Money = Money.ZERO,
    val unfundedExpenses: Money = Money.ZERO,
    val closingNetWorth: Money = totalNetWorth,
)

data class PoolYearFlow(
    val contribution: Money = Money.ZERO,
    val withdrawal: Money = Money.ZERO,
    val withdrawalShortfall: Money = Money.ZERO,
)

data class HouseholdProjectionResult(
    val householdId: String,
    val householdName: String,
    val timeline: List<YearCategoryBreakdown>,
    val finalNetWorth: Money,
    val p10Path: List<Money>,
    val p50Path: List<Money>,
    val p90Path: List<Money>,
    val successRate: Double,
)

data class ConsolidatedPlanResult(
    val timeline: List<YearCategoryBreakdown>,
    val householdResults: List<HouseholdProjectionResult>,
    val finalNetWorth: Money,
    val p10Path: List<Money>,
    val p50Path: List<Money>,
    val p90Path: List<Money>,
    val overallSuccessRate: Double,
)

/**
 * All balances and cash flows use nominal dollars. A timeline row contains opening
 * balances and that year's actual flows; its closing balances open the next row.
 * The final row is a terminal snapshot, with no additional year of spending.
 */
object MultiAssetEngine {
    private val withdrawalOrder = listOf(
        AssetCategory.CASH_EMERGENCY,
        AssetCategory.TAXABLE_BROKERAGE,
        AssetCategory.PRE_TAX_401K,
        AssetCategory.ROTH_IRA,
        AssetCategory.OTHER,
    )

    private class Projection(val household: Household) {
        val pools = household.allAssetPools()
        val expenses = household.allExpenses().associateBy { it.id }
        val rules = household.activePriorityRules().filter { it.enabled }
        val homes = household.lifeEvents.filterIsInstance<HomePurchaseTemplate>()
        val balances = pools.associate { it.id to it.currentBalance.value }.toMutableMap()
        val mortgages = mutableMapOf<String, Long>()
        val acquiredHomes = mutableMapOf<String, Int>()
        var cash = 0L
        var failed = false

        fun snapshot(year: Int): YearCategoryBreakdown {
            val assets = AssetCategory.entries.associateWith { category ->
                Money(
                    pools.filter { it.category == category }.sumOf { balances.getValue(it.id) } +
                        if (category == AssetCategory.CASH_EMERGENCY) cash else 0L,
                )
            }
            val liabilities = Money(mortgages.values.sum())
            return YearCategoryBreakdown(
                yearIndex = year,
                calendarYear = household.baseYear + year,
                age = household.currentAge + year,
                assetBalances = assets,
                expenseBalances = ExpenseCategory.entries.associateWith { Money.ZERO },
                totalNetWorth = Money(assets.values.sumOf { it.value } - liabilities.value),
                totalIncome = Money.ZERO,
                totalExpenses = Money.ZERO,
                netCashFlow = Money.ZERO,
                poolBalances = balances.mapValues { Money(it.value) },
                totalLiabilities = liabilities,
            )
        }

        /** Shared deterministic/stochastic annual ledger. Withdrawals are transfers, not income. */
        fun step(year: Int, inflation: Double, returnFor: (AssetPool, Int) -> Double): YearCategoryBreakdown {
            val opening = snapshot(year)
            val calendarYear = opening.calendarYear
            val primary = household.primaryEntity()
            val flows = mutableMapOf<String, PoolYearFlow>()
            val fundings = mutableListOf<YearlyItemFunding>()
            val expenseTotals = ExpenseCategory.entries.associateWith { 0L }.toMutableMap()

            // Grow only opening assets. Contributions occur at year end in every mode.
            for (pool in pools) {
                val rate = returnFor(pool, calendarYear)
                require(rate.isFinite()) { "Investment return must be finite" }
                val balance = balances.getValue(pool.id)
                balances[pool.id] = max(0L, balance + (balance * max(-1.0, rate)).toLong())
            }
            for (home in homes.filter { it.id in acquiredHomes }) {
                val debt = mortgages.getValue(home.id)
                mortgages[home.id] = debt + (debt * home.mortgageInterestRate).toLong()
            }

            val income = household.totalIncomeInYear(calendarYear, inflation).value
            require(income >= 0L) { "Scheduled income must be nonnegative" }
            var available = income + cash
            cash = 0L
            for (pool in pools) {
                val owner = household.findEntity(pool.entityId) ?: primary
                val target = pool.targetWithdrawalInYear(calendarYear, owner).value
                val actual = min(target, balances.getValue(pool.id))
                balances[pool.id] = balances.getValue(pool.id) - actual
                available += actual
                flows[pool.id] = PoolYearFlow(withdrawal = Money(actual), withdrawalShortfall = Money(target - actual))
            }

            fun drawAssets(amount: Long): Long {
                var remaining = amount
                for (category in withdrawalOrder) {
                    for (pool in pools.filter { it.category == category }) {
                        val drawn = min(remaining, balances.getValue(pool.id))
                        balances[pool.id] = balances.getValue(pool.id) - drawn
                        val old = flows.getValue(pool.id)
                        flows[pool.id] = old.copy(withdrawal = old.withdrawal + Money(drawn))
                        remaining -= drawn
                        if (remaining == 0L) return amount
                    }
                }
                return amount - remaining
            }

            fun funding(id: String, name: String, entityId: String, type: PriorityTargetType, itemType: PriorityItemType, target: Long, actual: Long) {
                fundings.add(
                    YearlyItemFunding(
                        id, name, entityId, type, itemType, Money(target), Money(actual), Money(target - actual),
                        when {
                            actual == target -> FundingStatus.FULLY_FUNDED
                            actual > 0L -> FundingStatus.PARTIALLY_FUNDED
                            else -> FundingStatus.UNFUNDED
                        },
                    ),
                )
            }

            for (rule in rules) {
                when (rule.targetType) {
                    PriorityTargetType.EXPENSE_PAYOUT -> {
                        val expense = expenses[rule.targetId] ?: continue
                        val purchase = homes.firstOrNull { "${it.id}_downpayment" == expense.id }
                        val mortgage = homes.firstOrNull { "${it.id}_mortgage" == expense.id }
                        var target = expense.amountInYear(calendarYear, household.baseYear, household.findEntity(expense.entityId), inflation).value
                        if (purchase != null) {
                            target = if (purchase.id in acquiredHomes) 0L else (purchase.propertyValue.value * purchase.downPaymentPercent).toLong()
                        }
                        if (mortgage != null) {
                            target = if (mortgage.id in acquiredHomes && calendarYear > acquiredHomes.getValue(mortgage.id)) {
                                val debt = mortgages.getValue(mortgage.id)
                                val atMaturity = calendarYear >= acquiredHomes.getValue(mortgage.id) + mortgage.loanTermYears
                                val debtPayment = if (atMaturity) debt else min(debt, mortgage.annualMortgageAndTax.value - mortgage.annualPropertyCosts.value)
                                debtPayment + mortgage.annualPropertyCosts.value
                            } else {
                                0L
                            }
                        }
                        if (target <= 0L && purchase == null) continue
                        if (purchase != null && (purchase.id in acquiredHomes || calendarYear != primary.yearAtAge(purchase.parentAgeAtPurchase))) continue
                        // Purchases are atomic: an unfunded down payment cannot create equity.
                        val liquid = pools.filter { it.category in withdrawalOrder }.sumOf { balances.getValue(it.id) }
                        val canPurchase = purchase == null || available + liquid >= target
                        val fromCash = if (canPurchase) min(available, target) else 0L
                        available -= fromCash
                        val paid = fromCash + if (canPurchase) drawAssets(target - fromCash) else 0L
                        expenseTotals[expense.category] = expenseTotals.getValue(expense.category) + target
                        funding(expense.id, expense.name, expense.entityId, rule.targetType, rule.itemType, target, paid)
                        if (paid < target) failed = true
                        if (purchase != null && paid == target) {
                            acquiredHomes[purchase.id] = calendarYear
                            balances["${purchase.id}_asset"] = purchase.propertyValue.value
                            mortgages[purchase.id] = purchase.propertyValue.value - target
                        }
                        if (mortgage != null && mortgage.id in acquiredHomes) {
                            // Property costs are paid before servicing the mortgage. Unpaid interest remains debt.
                            val debtPayment = max(0L, paid - mortgage.annualPropertyCosts.value)
                            mortgages[mortgage.id] = max(0L, mortgages.getValue(mortgage.id) - debtPayment)
                        }
                    }
                    PriorityTargetType.POOL_CONTRIBUTION -> {
                        val pool = pools.firstOrNull { it.id == rule.targetId } ?: continue
                        val target = pool.targetContributionInYear(calendarYear, household.findEntity(pool.entityId)).value
                        if (target <= 0L) continue
                        val invested = min(available, target)
                        available -= invested
                        balances[pool.id] = balances.getValue(pool.id) + invested
                        flows[pool.id] = flows.getValue(pool.id).copy(contribution = Money(invested))
                        funding(pool.id, pool.name, pool.entityId, rule.targetType, rule.itemType, target, invested)
                    }
                    PriorityTargetType.SURPLUS_INVESTMENT -> Unit
                }
            }
            // Retain every dollar of surplus, including retirement income and unspent withdrawals.
            val hasScheduledWithdrawal = flows.values.any { it.withdrawal.value > 0L }
            val explicitSurplusPool = rules.firstOrNull { it.targetType == PriorityTargetType.SURPLUS_INVESTMENT }
                ?.let { rule -> pools.firstOrNull { it.id == rule.targetId } }
            val surplusPool = explicitSurplusPool ?: if (opening.age < primary.retirementAge && !hasScheduledWithdrawal) {
                pools.firstOrNull { it.category == AssetCategory.TAXABLE_BROKERAGE }
                    ?: pools.firstOrNull { it.category == AssetCategory.CASH_EMERGENCY }
            } else {
                pools.firstOrNull { it.category == AssetCategory.CASH_EMERGENCY }
            }
            if (surplusPool != null) {
                balances[surplusPool.id] = balances.getValue(surplusPool.id) + available
                val old = flows.getValue(surplusPool.id)
                flows[surplusPool.id] = old.copy(contribution = old.contribution + Money(available))
            } else {
                cash = available
            }

            val totalExpenses = expenseTotals.values.sum()
            return opening.copy(
                expenseBalances = expenseTotals.mapValues { Money(it.value) },
                totalIncome = Money(income),
                totalExpenses = Money(totalExpenses),
                netCashFlow = Money(income - totalExpenses),
                itemFundings = fundings,
                unfundedCount = fundings.count { it.status.isProblem } + flows.values.count { it.withdrawalShortfall.value > 0L },
                poolFlows = flows,
                unfundedExpenses = Money(fundings.filter { it.targetType == PriorityTargetType.EXPENSE_PAYOUT }.sumOf { it.shortfall.value }),
                closingNetWorth = snapshot(year + 1).totalNetWorth,
            )
        }
    }

    private fun validate(household: Household, inflationRate: Double) {
        require(inflationRate.isFinite() && inflationRate > -1.0) { "Inflation must be finite and greater than -100%" }
        require(household.lifeExpectancy >= household.currentAge) { "Life expectancy must not precede current age" }
        val pools = household.allAssetPools()
        val expenses = household.allExpenses()
        require(household.incomeStreams.all { stream -> stream.yearlyPayBumpRate.isFinite() && stream.yearlyPayBumpRate >= -1.0 }) { "Invalid income growth rate" }
        require(pools.map { it.id }.distinct().size == pools.size) { "Asset pool IDs must be unique" }
        require(expenses.map { it.id }.distinct().size == expenses.size) { "Expense IDs must be unique" }
        require((pools.map { it.id } + expenses.map { it.id }).distinct().size == pools.size + expenses.size) { "Pool and expense IDs must not overlap" }
        require(household.activePriorityRules().map { it.targetType to it.targetId }.distinct().size == household.activePriorityRules().size) { "Priority targets must be unique" }
        require(household.annualIncome.value >= 0L && household.incomeStreams.all { it.initialAnnualAmount.value >= 0L }) { "Income must be nonnegative" }
        require(pools.all { it.currentBalance.value >= 0L && it.returnVolatility.isFinite() && it.returnVolatility >= 0.0 && it.expectedNominalReturn.isFinite() && it.expectedNominalReturn >= -1.0 }) { "Invalid asset balance, return or volatility" }
        for (home in household.lifeEvents.filterIsInstance<HomePurchaseTemplate>()) {
            require(home.parentAgeAtPurchase >= household.currentAge) { "Use existing asset pools for properties purchased before the plan starts" }
            require(home.downPaymentPercent in 0.0..1.0 && home.propertyValue.value > 0L && home.loanTermYears > 0) { "Invalid home purchase" }
            require(home.mortgageInterestRate.isFinite() && home.mortgageInterestRate >= 0.0 && home.annualPropertyCosts.value >= 0L && home.annualMortgageAndTax >= home.annualPropertyCosts) { "Invalid mortgage payment or interest rate" }
        }
    }

    fun simulateHousehold(household: Household, inflationRate: Double = 0.025): List<YearCategoryBreakdown> {
        validate(household, inflationRate)
        val projection = Projection(household)
        val years = household.lifeExpectancy - household.currentAge
        return (0 until years).map { year ->
            projection.step(year, inflationRate) { pool, calendarYear -> pool.expectedReturnInYear(calendarYear, household.findEntity(pool.entityId)) }
        } + projection.snapshot(years)
    }

    private fun gaussian(random: Random): Double = sqrt(-2.0 * ln(max(1e-10, random.nextDouble()))) * cos(2.0 * kotlin.math.PI * random.nextDouble())

    private fun percentile(paths: Array<LongArray>, year: Int, fraction: Double): Money {
        val values = LongArray(paths.size) { paths[it][year] }.apply { sort() }
        return Money(values[((values.size - 1) * fraction).toInt()])
    }

    fun runHouseholdMonteCarlo(
        household: Household,
        simulationsCount: Int = 500,
        inflationRate: Double = 0.025,
        randomSeed: Long? = null,
    ): HouseholdProjectionResult = simulatePlan(
        FinancialPlan(baseYear = household.baseYear, households = listOf(household), inflationRate = inflationRate),
        simulationsCount,
        randomSeed,
    ).householdResults.single()

    /**
     * Joint runs share a market shock per calendar year. Correlation is the common
     * variance fraction (0 = independent, 1 = identical standardized shocks).
     * Success means all scheduled expenses were funded in every household.
     * Balances are carried forward after a household's terminal snapshot; no later
     * income or spending is assumed and households do not cross-fund one another.
     */
    fun simulatePlan(
        plan: FinancialPlan,
        simulationsCount: Int = 500,
        randomSeed: Long? = null,
        returnCorrelation: Double = 0.75,
    ): ConsolidatedPlanResult {
        require(simulationsCount > 0) { "Simulation count must be positive" }
        require(returnCorrelation.isFinite() && returnCorrelation in 0.0..1.0) { "Correlation must be between zero and one" }
        require(plan.households.map { it.id }.distinct().size == plan.households.size) { "Household IDs must be unique" }
        plan.households.forEach {
            validate(it, plan.inflationRate)
            require(it.baseYear == plan.baseYear) { "Households must use the plan's base year" }
        }
        val timelines = plan.households.map { simulateHousehold(it, plan.inflationRate) }
        val years = timelines.maxOfOrNull { it.size - 1 } ?: 0
        val paths = plan.households.map { Array(simulationsCount) { LongArray(years + 1) } }
        val combined = Array(simulationsCount) { LongArray(years + 1) }
        val successes = IntArray(plan.households.size)
        var successfulPlans = 0
        val random = randomSeed?.let { Random(it) } ?: Random.Default
        repeat(simulationsCount) { sim ->
            val states = plan.households.map { Projection(it) }
            for (year in 0..years) {
                states.forEachIndexed { index, state ->
                    val worth = state.snapshot(year).totalNetWorth.value
                    paths[index][sim][year] = worth
                    combined[sim][year] += worth
                }
                if (year < years) {
                    val market = gaussian(random)
                    states.forEach { state ->
                        if (year < state.household.lifeExpectancy - state.household.currentAge) {
                            state.step(year, plan.inflationRate) { pool, calendarYear ->
                                val shock = sqrt(returnCorrelation) * market + sqrt(1.0 - returnCorrelation) * gaussian(random)
                                pool.expectedReturnInYear(calendarYear, state.household.findEntity(pool.entityId)) + pool.returnVolatility * shock
                            }
                        }
                    }
                }
            }
            states.forEachIndexed { i, state -> if (!state.failed) successes[i]++ }
            if (states.none { it.failed }) successfulPlans++
        }
        val householdResults = plan.households.mapIndexed { i, household ->
            val length = timelines[i].size
            HouseholdProjectionResult(
                household.id,
                household.name,
                timelines[i],
                timelines[i].last().totalNetWorth,
                List(length) { percentile(paths[i], it, 0.10) },
                List(length) { percentile(paths[i], it, 0.50) },
                List(length) { percentile(paths[i], it, 0.90) },
                successes[i] * 100.0 / simulationsCount,
            )
        }
        if (householdResults.isEmpty()) return ConsolidatedPlanResult(emptyList(), emptyList(), Money.ZERO, emptyList(), emptyList(), emptyList(), 100.0)
        val timeline = (0..years).map { year ->
            val rows = timelines.map { it.getOrNull(year) ?: it.last().copy(totalIncome = Money.ZERO, totalExpenses = Money.ZERO, netCashFlow = Money.ZERO, itemFundings = emptyList(), unfundedCount = 0) }
            val fundings = rows.flatMap { it.itemFundings }
            YearCategoryBreakdown(
                year, plan.baseYear + year, (plan.households.firstOrNull { it.isPrimary } ?: plan.households.first()).currentAge + year,
                AssetCategory.entries.associateWith { cat -> Money(rows.sumOf { it.assetBalances[cat]?.value ?: 0L }) },
                ExpenseCategory.entries.associateWith { cat -> Money(rows.sumOf { it.expenseBalances[cat]?.value ?: 0L }) },
                Money(rows.sumOf { it.totalNetWorth.value }), Money(rows.sumOf { it.totalIncome.value }),
                Money(rows.sumOf { it.totalExpenses.value }), Money(rows.sumOf { it.netCashFlow.value }), fundings,
                unfundedCount = rows.sumOf { it.unfundedCount },
                totalLiabilities = Money(rows.sumOf { it.totalLiabilities.value }),
                unfundedExpenses = Money(rows.sumOf { it.unfundedExpenses.value }),
                closingNetWorth = Money(rows.sumOf { it.closingNetWorth.value }),
            )
        }
        return ConsolidatedPlanResult(
            timeline,
            householdResults,
            timeline.last().totalNetWorth,
            List(years + 1) { percentile(combined, it, 0.10) },
            List(years + 1) { percentile(combined, it, 0.50) },
            List(years + 1) { percentile(combined, it, 0.90) },
            successfulPlans * 100.0 / simulationsCount,
        )
    }
}
