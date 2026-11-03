package com.muandrew.forecast

import com.muandrew.forecast.engine.MultiAssetEngine
import com.muandrew.forecast.model.AssetCategory
import com.muandrew.forecast.model.AssetPool
import com.muandrew.forecast.model.AssetPoolOverride
import com.muandrew.forecast.model.Entity
import com.muandrew.forecast.model.ExpenseCategory
import com.muandrew.forecast.model.ExpenseItem
import com.muandrew.forecast.model.FinancialPlan
import com.muandrew.forecast.model.FundingStatus
import com.muandrew.forecast.model.HomePurchaseTemplate
import com.muandrew.forecast.model.Household
import com.muandrew.forecast.model.IncomeStream
import com.muandrew.forecast.model.Money
import com.muandrew.forecast.model.PriorityItemType
import com.muandrew.forecast.model.PriorityRule
import com.muandrew.forecast.model.PriorityTargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProjectionAccountingTest {
    private fun dollars(value: Long) = Money.ofDollars(value)
    private fun pool(balance: Long = 1_000, rate: Double = 0.0, volatility: Double = 0.0) = AssetPool(
        "brokerage",
        "Brokerage",
        AssetCategory.TAXABLE_BROKERAGE,
        dollars(balance),
        expectedNominalReturn = rate,
        returnVolatility = volatility,
    )
    private fun household(
        pools: List<AssetPool> = listOf(pool()),
        income: Long = 0,
        expense: Long = 0,
        years: Int = 2,
        retired: Boolean = false,
    ) = Household(
        id = "household",
        name = "Family",
        baseYear = 2026,
        entities = listOf(Entity("person", "Person", 1996, true, if (retired) 30 else 65, 30 + years)),
        annualIncome = dollars(income),
        assetPools = pools,
        expenses = if (expense == 0L) {
            emptyList()
        } else {
            listOf(
                ExpenseItem(
                    "living",
                    "Living",
                    ExpenseCategory.LIVING_ESSENTIALS,
                    dollars(expense),
                    inflationAdjusted = false,
                ),
            )
        },
    )

    @Test
    fun nominalReturnsAreNotDeflatedWhileExpensesInflate() {
        val home = household(pools = listOf(pool(rate = 0.10)), expense = 100).let {
            it.copy(expenses = it.expenses.map { exp -> exp.copy(inflationAdjusted = true) })
        }
        val rows = MultiAssetEngine.simulateHousehold(home, 0.10)
        assertEquals(dollars(1_000), rows[1].totalNetWorth)
        assertEquals(dollars(110), rows[1].totalExpenses)
        assertEquals(dollars(990), rows.last().totalNetWorth)
    }

    @Test
    fun zeroVolatilityMatchesDeterministicPathIncludingOverridesAndContributions() {
        val asset = pool(rate = 0.10).copy(
            annualContribution = dollars(20),
            overrides = listOf(
                AssetPoolOverride(startAge = 31, expectedNominalReturn = 0.20),
            ),
        )
        val home = household(listOf(asset), income = 100, expense = 70, years = 3)
        val result = MultiAssetEngine.runHouseholdMonteCarlo(home, 10, 0.025, 42)
        assertEquals(result.timeline.map { it.totalNetWorth }, result.p50Path)
        assertEquals(result.p10Path, result.p90Path)
        assertEquals(dollars(1_130), result.timeline[1].totalNetWorth)
    }

    @Test
    fun contributionsBeforeExpensesDoNotCreateMoney() {
        val home = household(listOf(pool(balance = 0).copy(annualContribution = dollars(20))), income = 100, expense = 100).copy(
            priorityRules = listOf(
                PriorityRule("invest", "Invest", PriorityTargetType.POOL_CONTRIBUTION, "brokerage", itemType = PriorityItemType.CUSTOM_POOL, priorityRank = 1),
                PriorityRule("spend", "Spend", PriorityTargetType.EXPENSE_PAYOUT, "living", itemType = PriorityItemType.EXPENSE_ESSENTIAL, priorityRank = 2),
            ),
        )
        val row = MultiAssetEngine.simulateHousehold(home, 0.0).first()
        assertEquals(dollars(0), row.closingNetWorth)
        assertEquals(dollars(100), row.itemFundings.single { it.id == "living" }.actualAmount)
        assertEquals(dollars(20), row.poolFlows.getValue("brokerage").withdrawal)
        assertEquals(Money.ZERO, row.unfundedExpenses)
    }

    @Test
    fun retirementExpensesAreFundedFromAssetsBeforeStatusIsReported() {
        val result = MultiAssetEngine.runHouseholdMonteCarlo(household(expense = 100, retired = true), 5, 0.0, 42)
        assertEquals(100.0, result.successRate)
        assertEquals(FundingStatus.FULLY_FUNDED, result.timeline.first().itemFundings.single().status)
        assertEquals(dollars(800), result.finalNetWorth)
    }

    @Test
    fun exhaustedLiquidAssetsFailEvenWhenPropertyRemains() {
        val property = AssetPool("house", "House", AssetCategory.REAL_ESTATE, dollars(1_000_000), expectedNominalReturn = 0.0, returnVolatility = 0.0)
        val result = MultiAssetEngine.runHouseholdMonteCarlo(household(listOf(property), expense = 100, retired = true), 5, 0.0, 42)
        assertEquals(0.0, result.successRate)
        assertEquals(dollars(100), result.timeline.first().unfundedExpenses)
        assertEquals(dollars(1_000_000), result.finalNetWorth)
    }

    @Test
    fun zeroWealthIsSuccessfulWhenAllExpensesArePaid() {
        val result = MultiAssetEngine.runHouseholdMonteCarlo(household(listOf(pool(100)), expense = 100, years = 1), 5, 0.0, 42)
        assertEquals(100.0, result.successRate)
        assertEquals(Money.ZERO, result.finalNetWorth)
        assertTrue(result.timeline.last().itemFundings.isEmpty())
        assertEquals(Money.ZERO, result.timeline.last().totalExpenses)
    }

    @Test
    fun scheduledWithdrawalsTransferToCashAndReportActualShortfalls() {
        val asset = pool(100).copy(annualFlow = dollars(-150))
        val result = MultiAssetEngine.runHouseholdMonteCarlo(household(listOf(asset), expense = 80, years = 1, retired = true), 5, 0.0, 42)
        val first = result.timeline.first()
        assertEquals(dollars(100), first.poolFlows.getValue(asset.id).withdrawal)
        assertEquals(dollars(50), first.poolFlows.getValue(asset.id).withdrawalShortfall)
        assertEquals(dollars(20), result.finalNetWorth)
        assertEquals(dollars(20), result.timeline.last().assetBalances[AssetCategory.CASH_EMERGENCY])
        assertEquals(Money.ZERO, result.timeline.last().poolBalances[asset.id])
    }

    @Test
    fun retirementSurplusAndPlansWithoutPoolsRetainCash() {
        val income = IncomeStream("pension", "Pension", dollars(100), yearlyPayBumpRate = 0.0, inflationAdjusted = false)
        val home = household(emptyList(), expense = 80, retired = true).copy(incomeStreams = listOf(income))
        val rows = MultiAssetEngine.simulateHousehold(home, 0.0)
        assertEquals(dollars(40), rows.last().totalNetWorth)
        assertEquals(dollars(40), rows.last().assetBalances[AssetCategory.CASH_EMERGENCY])
    }

    @Test
    fun scheduledFlowStartsAndLegacyEndAgesAreRespected() {
        val person = Entity("person", "Person", 1996, true, 60)
        val asset = pool().copy(startAge = 32, annualContribution = dollars(10), contributionEndAge = 34)
        assertEquals(Money.ZERO, asset.targetContributionInYear(2027, person))
        assertEquals(dollars(10), asset.targetContributionInYear(2028, person))
        assertEquals(Money.ZERO, asset.targetContributionInYear(2030, person))
        val draw = pool().copy(annualWithdrawal = dollars(10), withdrawalStartAge = 35, withdrawalEndAge = 36)
        assertEquals(Money.ZERO, draw.targetWithdrawalInYear(2030, person))
        assertEquals(dollars(10), draw.targetWithdrawalInYear(2031, person))
        assertEquals(Money.ZERO, draw.targetWithdrawalInYear(2033, person))
    }

    @Test
    fun homeIsAcquiredAtPurchaseWithMortgageAndNoArtificialWealth() {
        val home = HomePurchaseTemplate(
            "home",
            "Home",
            31,
            dollars(500_000),
            0.20,
            annualMortgageAndTax = dollars(30_000),
            mortgageInterestRate = 0.0,
        )
        val family = household(listOf(pool(200_000)), years = 3).copy(lifeEvents = listOf(home))
        val rows = MultiAssetEngine.simulateHousehold(family, 0.0)
        assertEquals(dollars(200_000), rows[0].totalNetWorth)
        assertEquals(Money.ZERO, rows[1].assetBalances[AssetCategory.REAL_ESTATE])
        assertEquals(dollars(200_000), rows[2].totalNetWorth)
        assertEquals(dollars(400_000), rows[2].totalLiabilities)
        assertEquals(dollars(370_000), rows[3].totalLiabilities)
        // Property appreciation starts only after acquisition; principal is a transfer to equity.
        assertEquals(dollars(222_500), rows[3].totalNetWorth)
    }

    @Test
    fun mortgageInterestAndPropertyCostsReduceWealthButPrincipalDoesNot() {
        val home = HomePurchaseTemplate(
            "home",
            "Home",
            30,
            dollars(500_000),
            0.20,
            annualMortgageAndTax = dollars(30_000),
            mortgageInterestRate = 0.05,
            annualPropertyCosts = dollars(5_000),
        )
        val family = household(listOf(pool(200_000)), years = 2).copy(lifeEvents = listOf(home))
        val rows = MultiAssetEngine.simulateHousehold(family, 0.0)
        assertEquals(dollars(395_000), rows.last().totalLiabilities)
        assertEquals(dollars(197_500), rows.last().totalNetWorth)
    }

    @Test
    fun unfundedPurchaseNeverCreatesAPropertyOrLosesPartialDownPayment() {
        val home = HomePurchaseTemplate("home", "Home", 30, dollars(500_000), 0.20)
        val family = household(listOf(pool(50_000)), years = 2).copy(lifeEvents = listOf(home))
        val result = MultiAssetEngine.runHouseholdMonteCarlo(family, 5, 0.0, 42)
        assertEquals(0.0, result.successRate)
        assertEquals(dollars(50_000), result.finalNetWorth)
        assertTrue(result.timeline.all { it.assetBalances[AssetCategory.REAL_ESTATE] == Money.ZERO })
        assertEquals(Money.ZERO, result.timeline.last().totalLiabilities)
    }

    @Test
    fun mortgageMaturityPaysRemainingDebtAndPropertyCostsContinue() {
        val home = HomePurchaseTemplate(
            "home", "Home", 30, dollars(1_000), 0.20,
            annualMortgageAndTax = dollars(110), loanTermYears = 1, mortgageInterestRate = 0.0, annualPropertyCosts = dollars(10),
        )
        val rows = MultiAssetEngine.simulateHousehold(household(listOf(pool(2_000)), years = 3).copy(lifeEvents = listOf(home)), 0.0)
        assertEquals(dollars(810), rows[1].totalExpenses)
        assertEquals(Money.ZERO, rows[2].totalLiabilities)
        assertEquals(dollars(10), rows[2].totalExpenses)
    }

    @Test
    fun consolidatedSuccessRequiresEveryHouseholdToSucceed() {
        val funded = household().copy(id = "funded")
        val unfunded = household(emptyList(), expense = 100).copy(id = "unfunded")
        val result = MultiAssetEngine.simulatePlan(FinancialPlan(households = listOf(funded, unfunded)), 10, 42)
        assertEquals(listOf(100.0, 0.0), result.householdResults.map { it.successRate })
        assertEquals(0.0, result.overallSuccessRate)
    }

    @Test
    fun consolidatedQuantilesUseJointOutcomesInsteadOfSummingQuantiles() {
        val first = household(listOf(pool(100_000, 0.05, 0.20)), years = 1)
        val second = first.copy(id = "second")
        val result = MultiAssetEngine.simulatePlan(FinancialPlan(households = listOf(first, second)), 1000, 42, returnCorrelation = 0.0)
        val sumOfP10s = result.householdResults.sumOf { it.p10Path.last().value }
        assertTrue(result.p10Path.last().value > sumOfP10s)
        assertEquals(result, MultiAssetEngine.simulatePlan(FinancialPlan(households = listOf(first, second)), 1000, 42, returnCorrelation = 0.0))
    }

    @Test
    fun sharedMarketShockCorrelatesHouseholdsAndTerminalAssetsAreRetained() {
        val short = household(listOf(pool(100_000, 0.05, 0.20)), years = 1)
        val same = short.copy(id = "same")
        val joint = MultiAssetEngine.simulatePlan(FinancialPlan(households = listOf(short, same)), 100, 42, returnCorrelation = 1.0)
        assertEquals(joint.householdResults[0].p10Path, joint.householdResults[1].p10Path)
        val long = household(listOf(pool(500)), years = 3).copy(id = "long")
        val result = MultiAssetEngine.simulatePlan(FinancialPlan(households = listOf(household(years = 1), long)), 10, 42)
        assertEquals(dollars(1_500), result.finalNetWorth)
        assertEquals(result.finalNetWorth, result.p50Path.last())
    }

    @Test
    fun cashConservationHoldsAcrossPriorityOrdersAndDeficits() {
        for (initial in listOf(0L, 20L, 100L)) {
            for (income in listOf(0L, 50L, 100L)) {
                for (expense in listOf(50L, 100L, 200L)) {
                    for (investFirst in listOf(false, true)) {
                        val home = household(listOf(pool(initial).copy(annualContribution = dollars(30))), income, expense).copy(
                            priorityRules = listOf(
                                PriorityRule("invest", "Invest", PriorityTargetType.POOL_CONTRIBUTION, "brokerage", itemType = PriorityItemType.CUSTOM_POOL, priorityRank = if (investFirst) 1 else 2),
                                PriorityRule("spend", "Spend", PriorityTargetType.EXPENSE_PAYOUT, "living", itemType = PriorityItemType.CUSTOM_EXPENSE, priorityRank = if (investFirst) 2 else 1),
                            ),
                        )
                        val rows = MultiAssetEngine.simulateHousehold(home, 0.0)
                        rows.dropLast(1).forEachIndexed { year, row ->
                            val actualExpenses = row.totalExpenses - row.unfundedExpenses
                            assertEquals(row.totalNetWorth + row.totalIncome - actualExpenses, row.closingNetWorth)
                            assertEquals(row.closingNetWorth, rows[year + 1].totalNetWorth)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun legacyContributionSwitchesToWithdrawalAndExplicitPhasesStop() {
        val person = Entity("person", "Person", 1996, true, 60)
        val asset = pool().copy(annualContribution = dollars(10), annualWithdrawal = dollars(20), withdrawalStartAge = 35, contributionEndAge = 35)
        assertEquals(dollars(10), asset.targetContributionInYear(2026, person))
        assertEquals(dollars(20), asset.targetWithdrawalInYear(2031, person))
        val phased = asset.copy(phases = listOf(com.muandrew.forecast.model.SchedulePhase(startAge = 30, endAge = 31, amount = dollars(15))))
        assertEquals(Money.ZERO, phased.targetContributionInYear(2028, person))
    }

    @Test
    fun invalidSimulationCountsAndMismatchedCalendarsFailClearly() {
        assertFailsWith<IllegalArgumentException> { MultiAssetEngine.runHouseholdMonteCarlo(household(), 0) }
        assertFailsWith<IllegalArgumentException> { MultiAssetEngine.simulateHousehold(household(), Double.NaN) }
        assertFailsWith<IllegalArgumentException> { MultiAssetEngine.simulatePlan(FinancialPlan(households = listOf(household().copy(baseYear = 2027)))) }
    }
}
