package com.muandrew.forecast.engine

import com.muandrew.forecast.model.AssetCategory
import com.muandrew.forecast.model.AssetPool
import com.muandrew.forecast.model.ExpenseCategory
import com.muandrew.forecast.model.ExpenseItem
import com.muandrew.forecast.model.FinancialPlan
import com.muandrew.forecast.model.Household
import com.muandrew.forecast.model.Money
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
    val netCashFlow: Money
)

data class HouseholdProjectionResult(
    val householdId: String,
    val householdName: String,
    val timeline: List<YearCategoryBreakdown>,
    val finalNetWorth: Money,
    val p10Path: List<Money>,
    val p50Path: List<Money>,
    val p90Path: List<Money>,
    val successRate: Double
)

data class ConsolidatedPlanResult(
    val timeline: List<YearCategoryBreakdown>,
    val householdResults: List<HouseholdProjectionResult>,
    val finalNetWorth: Money,
    val p10Path: List<Money>,
    val p50Path: List<Money>,
    val p90Path: List<Money>,
    val overallSuccessRate: Double
)

object MultiAssetEngine {

    /**
     * Deterministic simulation calculating exact category-by-category breakdown over time.
     */
    fun simulateHousehold(
        household: Household,
        inflationRate: Double = 0.025
    ): List<YearCategoryBreakdown> {
        val totalYears = max(1, household.lifeExpectancy - household.currentAge)
        val timeline = mutableListOf<YearCategoryBreakdown>()

        val pools = household.allAssetPools()
        val allExpenses = household.allExpenses()

        // Track balance per pool (in cents)
        val currentBalances = pools.associate { it.id to it.currentBalance.value }.toMutableMap()

        for (year in 0..totalYears) {
            val calendarYear = household.baseYear + year
            val primary = household.primaryEntity()
            val age = primary.ageInYear(calendarYear)
            val isWorking = age < primary.retirementAge

            // 1. Calculate expenses for this year by category
            val expenseMap = mutableMapOf<ExpenseCategory, Long>()
            for (category in ExpenseCategory.entries) {
                expenseMap[category] = 0L
            }

            var totalYearExpenses = 0L
            for (expense in allExpenses) {
                val entity = household.findEntity(expense.entityId)
                val amount = expense.amountInYear(calendarYear, household.baseYear, entity, inflationRate).value
                if (amount > 0L) {
                    expenseMap[expense.category] = (expenseMap[expense.category] ?: 0L) + amount
                    totalYearExpenses += amount
                }
            }

            // 2. Calculate income for this year
            val yearIncome = household.totalIncomeInYear(calendarYear, inflationRate).value

            // 3. Snapshot current asset balances by category
            val assetMap = mutableMapOf<AssetCategory, Long>()
            for (category in AssetCategory.entries) {
                assetMap[category] = 0L
            }
            var totalYearAssets = 0L
            for (pool in pools) {
                val balance = max(0L, currentBalances[pool.id] ?: 0L)
                assetMap[pool.category] = (assetMap[pool.category] ?: 0L) + balance
                totalYearAssets += balance
            }

            val netCashFlow = yearIncome - totalYearExpenses

            timeline.add(
                YearCategoryBreakdown(
                    yearIndex = year,
                    calendarYear = calendarYear,
                    age = age,
                    assetBalances = assetMap.mapValues { Money(it.value) },
                    expenseBalances = expenseMap.mapValues { Money(it.value) },
                    totalNetWorth = Money(totalYearAssets),
                    totalIncome = Money(yearIncome),
                    totalExpenses = Money(totalYearExpenses),
                    netCashFlow = Money(netCashFlow)
                )
            )

            if (year < totalYears) {
                // Apply investment growth per pool
                for (pool in pools) {
                    val balance = currentBalances[pool.id] ?: 0L
                    if (balance > 0) {
                        val realReturn = (1.0 + pool.expectedNominalReturn) / (1.0 + inflationRate) - 1.0
                        val growth = (balance.toDouble() * realReturn).toLong()
                        currentBalances[pool.id] = balance + growth
                    }

                    // Apply annual contributions if applicable
                    val owner = household.findEntity(pool.entityId) ?: primary
                    val ownerAge = owner.ageInYear(calendarYear)
                    val endAge = pool.contributionEndAge ?: owner.retirementAge
                    if (ownerAge < endAge && pool.annualContribution.value > 0) {
                        currentBalances[pool.id] = (currentBalances[pool.id] ?: 0L) + pool.annualContribution.value
                    }
                }

                // Handle net deficit or surplus across pools
                if (netCashFlow < 0) {
                    var remainingDeficit = -netCashFlow
                    val withdrawalPriority = listOf(
                        AssetCategory.CASH_EMERGENCY,
                        AssetCategory.TAXABLE_BROKERAGE,
                        AssetCategory.PRE_TAX_401K,
                        AssetCategory.ROTH_IRA,
                        AssetCategory.OTHER
                    )

                    for (cat in withdrawalPriority) {
                        if (remainingDeficit <= 0) break
                        for (pool in pools.filter { it.category == cat }) {
                            val available = currentBalances[pool.id] ?: 0L
                            if (available > 0) {
                                val deduction = min(available, remainingDeficit)
                                currentBalances[pool.id] = available - deduction
                                remainingDeficit -= deduction
                                if (remainingDeficit <= 0) break
                            }
                        }
                    }
                } else if (netCashFlow > 0 && isWorking) {
                    val defaultPool = pools.firstOrNull { it.category == AssetCategory.TAXABLE_BROKERAGE }
                        ?: pools.firstOrNull { it.category == AssetCategory.CASH_EMERGENCY }
                        ?: pools.firstOrNull()

                    if (defaultPool != null) {
                        currentBalances[defaultPool.id] = (currentBalances[defaultPool.id] ?: 0L) + netCashFlow
                    }
                }
            }
        }

        return timeline
    }

    /**
     * Runs stochastic Monte Carlo simulations across all asset pools in a household.
     */
    fun runHouseholdMonteCarlo(
        household: Household,
        simulationsCount: Int = 500,
        inflationRate: Double = 0.025,
        randomSeed: Long? = null
    ): HouseholdProjectionResult {
        val random = if (randomSeed != null) Random(randomSeed) else Random.Default
        val totalYears = max(1, household.lifeExpectancy - household.currentAge)
        val pools = household.allAssetPools()
        val allExpenses = household.allExpenses()

        val allPathBalances = Array(simulationsCount) { LongArray(totalYears + 1) }
        var successfulSims = 0

        for (sim in 0 until simulationsCount) {
            val poolBalances = pools.associate { it.id to it.currentBalance.value }.toMutableMap()
            val initialTotal = poolBalances.values.sum()
            allPathBalances[sim][0] = initialTotal
            var failed = false

            for (year in 0 until totalYears) {
                val calendarYear = household.baseYear + year
                val primary = household.primaryEntity()
                val age = primary.ageInYear(calendarYear)
                val isWorking = age < primary.retirementAge

                // 1. Calculate expenses
                var yearExpenses = 0L
                for (exp in allExpenses) {
                    val entity = household.findEntity(exp.entityId)
                    yearExpenses += exp.amountInYear(calendarYear, household.baseYear, entity, inflationRate).value
                }

                // 2. Calculate income
                val yearIncome = household.totalIncomeInYear(calendarYear, inflationRate).value

                // 3. Investment growth with Box-Muller Gaussian noise per pool
                for (pool in pools) {
                    val bal = poolBalances[pool.id] ?: 0L
                    if (bal > 0) {
                        val u1 = max(1e-10, random.nextDouble())
                        val u2 = random.nextDouble()
                        val z0 = sqrt(-2.0 * ln(u1)) * cos(2.0 * kotlin.math.PI * u2)

                        val nominalReturn = pool.expectedNominalReturn + z0 * pool.returnVolatility
                        val realReturn = (1.0 + nominalReturn) / (1.0 + inflationRate) - 1.0
                        val growth = (bal.toDouble() * realReturn).toLong()
                        poolBalances[pool.id] = max(0L, bal + growth)
                    }

                    val owner = household.findEntity(pool.entityId) ?: primary
                    val ownerAge = owner.ageInYear(calendarYear)
                    val endAge = pool.contributionEndAge ?: owner.retirementAge
                    if (ownerAge < endAge && pool.annualContribution.value > 0) {
                        poolBalances[pool.id] = (poolBalances[pool.id] ?: 0L) + pool.annualContribution.value
                    }
                }

                // 4. Net cashflow
                val netCash = yearIncome - yearExpenses
                if (netCash < 0) {
                    var deficit = -netCash
                    val priority = listOf(
                        AssetCategory.CASH_EMERGENCY,
                        AssetCategory.TAXABLE_BROKERAGE,
                        AssetCategory.PRE_TAX_401K,
                        AssetCategory.ROTH_IRA,
                        AssetCategory.OTHER
                    )
                    for (cat in priority) {
                        if (deficit <= 0) break
                        for (p in pools.filter { it.category == cat }) {
                            val b = poolBalances[p.id] ?: 0L
                            if (b > 0) {
                                val d = min(b, deficit)
                                poolBalances[p.id] = b - d
                                deficit -= d
                                if (deficit <= 0) break
                            }
                        }
                    }
                } else if (netCash > 0 && isWorking) {
                    val defaultPool = pools.firstOrNull { it.category == AssetCategory.TAXABLE_BROKERAGE } ?: pools.firstOrNull()
                    if (defaultPool != null) {
                        poolBalances[defaultPool.id] = (poolBalances[defaultPool.id] ?: 0L) + netCash
                    }
                }

                val currentNetWorth = max(0L, poolBalances.values.sum())
                allPathBalances[sim][year + 1] = currentNetWorth
                if (currentNetWorth <= 0) {
                    failed = true
                }
            }

            if (!failed && allPathBalances[sim][totalYears] > 0) {
                successfulSims++
            }
        }

        // Calculate percentiles P10, P50, P90
        val p10List = mutableListOf<Money>()
        val p50List = mutableListOf<Money>()
        val p90List = mutableListOf<Money>()

        for (year in 0..totalYears) {
            val yearVals = LongArray(simulationsCount) { sim -> allPathBalances[sim][year] }
            yearVals.sort()
            val p10Idx = (simulationsCount * 0.10).toInt().coerceIn(0, simulationsCount - 1)
            val p50Idx = (simulationsCount * 0.50).toInt().coerceIn(0, simulationsCount - 1)
            val p90Idx = (simulationsCount * 0.90).toInt().coerceIn(0, simulationsCount - 1)

            p10List.add(Money(yearVals[p10Idx]))
            p50List.add(Money(yearVals[p50Idx]))
            p90List.add(Money(yearVals[p90Idx]))
        }

        val deterministicTimeline = simulateHousehold(household, inflationRate)

        return HouseholdProjectionResult(
            householdId = household.id,
            householdName = household.name,
            timeline = deterministicTimeline,
            finalNetWorth = deterministicTimeline.lastOrNull()?.totalNetWorth ?: Money.ZERO,
            p10Path = p10List,
            p50Path = p50List,
            p90Path = p90List,
            successRate = (successfulSims.toDouble() / simulationsCount.toDouble()) * 100.0
        )
    }

    /**
     * Consolidates projections across multiple households.
     */
    fun simulatePlan(
        plan: FinancialPlan,
        simulationsCount: Int = 500
    ): ConsolidatedPlanResult {
        val householdResults = plan.households.map {
            runHouseholdMonteCarlo(it, simulationsCount, plan.inflationRate)
        }

        if (householdResults.isEmpty()) {
            return ConsolidatedPlanResult(
                timeline = emptyList(),
                householdResults = emptyList(),
                finalNetWorth = Money.ZERO,
                p10Path = emptyList(),
                p50Path = emptyList(),
                p90Path = emptyList(),
                overallSuccessRate = 100.0
            )
        }

        val maxTimelineLength = householdResults.maxOfOrNull { it.timeline.size } ?: 0
        val consolidatedTimeline = mutableListOf<YearCategoryBreakdown>()
        val p10Consolidated = mutableListOf<Money>()
        val p50Consolidated = mutableListOf<Money>()
        val p90Consolidated = mutableListOf<Money>()

        val baseAge = plan.households.firstOrNull { it.isPrimary }?.currentAge
            ?: plan.households.firstOrNull()?.currentAge ?: 30

        for (year in 0 until maxTimelineLength) {
            val assetMap = mutableMapOf<AssetCategory, Long>()
            val expenseMap = mutableMapOf<ExpenseCategory, Long>()
            var totalNW = 0L
            var totalInc = 0L
            var totalExp = 0L
            var p10Total = 0L
            var p50Total = 0L
            var p90Total = 0L

            for (res in householdResults) {
                if (year < res.timeline.size) {
                    val bk = res.timeline[year]
                    bk.assetBalances.forEach { (cat, m) ->
                        assetMap[cat] = (assetMap[cat] ?: 0L) + m.value
                    }
                    bk.expenseBalances.forEach { (cat, m) ->
                        expenseMap[cat] = (expenseMap[cat] ?: 0L) + m.value
                    }
                    totalNW += bk.totalNetWorth.value
                    totalInc += bk.totalIncome.value
                    totalExp += bk.totalExpenses.value
                }
                if (year < res.p10Path.size) p10Total += res.p10Path[year].value
                if (year < res.p50Path.size) p50Total += res.p50Path[year].value
                if (year < res.p90Path.size) p90Total += res.p90Path[year].value
            }

            consolidatedTimeline.add(
                YearCategoryBreakdown(
                    yearIndex = year,
                    calendarYear = plan.baseYear + year,
                    age = baseAge + year,
                    assetBalances = assetMap.mapValues { Money(it.value) },
                    expenseBalances = expenseMap.mapValues { Money(it.value) },
                    totalNetWorth = Money(totalNW),
                    totalIncome = Money(totalInc),
                    totalExpenses = Money(totalExp),
                    netCashFlow = Money(totalInc - totalExp)
                )
            )

            p10Consolidated.add(Money(p10Total))
            p50Consolidated.add(Money(p50Total))
            p90Consolidated.add(Money(p90Total))
        }

        val avgSuccess = if (householdResults.isNotEmpty()) {
            householdResults.map { it.successRate }.average()
        } else 100.0

        return ConsolidatedPlanResult(
            timeline = consolidatedTimeline,
            householdResults = householdResults,
            finalNetWorth = consolidatedTimeline.lastOrNull()?.totalNetWorth ?: Money.ZERO,
            p10Path = p10Consolidated,
            p50Path = p50Consolidated,
            p90Path = p90Consolidated,
            overallSuccessRate = avgSuccess
        )
    }
}
