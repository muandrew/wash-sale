package com.muandrew.forecast

import com.muandrew.forecast.engine.MonteCarloEngine
import com.muandrew.forecast.engine.MultiAssetEngine
import com.muandrew.forecast.engine.RetirementCalculator
import com.muandrew.forecast.model.AssetCategory
import com.muandrew.forecast.model.AssetPool
import com.muandrew.forecast.model.ChildTemplate
import com.muandrew.forecast.model.ExpenseCategory
import com.muandrew.forecast.model.ExpenseItem
import com.muandrew.forecast.model.FinancialPlan
import com.muandrew.forecast.model.ForecastProfile
import com.muandrew.forecast.model.HomePurchaseTemplate
import com.muandrew.forecast.model.Household
import com.muandrew.forecast.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonteCarloTest {

    @Test
    fun testMoneyArithmetic() {
        val m1 = Money.ofDollars(100)
        val m2 = Money.ofDollars(50)
        val sum = m1 + m2
        assertEquals(15000L, sum.value)
        assertEquals("$150.00", sum.toFormattedString())

        val diff = m1 - m2
        assertEquals(5000L, diff.value)

        val div = m1 / 3
        assertEquals(Money.ofCents(3333), div.res)
        assertEquals(Money.ofCents(1), div.rem)

        val large = Money.ofDollars(1_234_567)
        assertEquals("$1,234,567.00", large.toFormattedString())

        val negativeLarge = Money.ofCents(-9_876_543_21L)
        assertEquals("($9,876,543.21)", negativeLarge.toFormattedString())
    }

    @Test
    fun testIncomeStreamPayBump() {
        val stream = com.muandrew.forecast.model.IncomeStream(
            id = "inc1",
            name = "Engineering Lead",
            initialAnnualAmount = Money.ofDollars(100_000),
            startAge = 30,
            endAge = 60,
            yearlyPayBumpRate = 0.05, // 5.0% yearly raise
            inflationAdjusted = false
        )

        // Age 30 (year 0): $100,000
        assertEquals(Money.ofDollars(100_000), stream.amountAtAge(30, baseAge = 30, inflationRate = 0.0))

        // Age 31 (year 1): $100,000 * 1.05 = $105,000
        assertEquals(Money.ofDollars(105_000), stream.amountAtAge(31, baseAge = 30, inflationRate = 0.0))

        // Age 32 (year 2): $100,000 * (1.05)^2 = $110,250
        assertEquals(Money.ofDollars(110_250), stream.amountAtAge(32, baseAge = 30, inflationRate = 0.0))

        // Age 65 (past endAge): $0
        assertEquals(Money.ZERO, stream.amountAtAge(65, baseAge = 30, inflationRate = 0.0))
    }

    @Test
    fun testEntityAndTimingModes() {
        val parent = com.muandrew.forecast.model.Entity(id = "p1", name = "Alex", birthYear = 1996, isPrimary = true)
        val child = com.muandrew.forecast.model.Entity(id = "c1", name = "Emma", birthYear = 2028, isPrimary = false)

        assertEquals(30, parent.ageInYear(2026))
        assertEquals(2056, parent.yearAtAge(60))

        assertEquals(0, child.ageInYear(2028))
        assertEquals(18, child.ageInYear(2046))

        // Income stream set by Entity Age
        val job = com.muandrew.forecast.model.IncomeStream(
            id = "job1",
            name = "Alex Career",
            entityId = parent.id,
            initialAnnualAmount = Money.ofDollars(150_000),
            timeMode = com.muandrew.forecast.model.TimeMode.ENTITY_AGE,
            startAge = 30,
            endAge = 60,
            yearlyPayBumpRate = 0.0
        )
        assertEquals(2026, job.effectiveStartYear(parent))
        assertEquals(2056, job.effectiveEndYear(parent))
        assertTrue(job.isApplicableInYear(2030, parent))
        assertTrue(!job.isApplicableInYear(2060, parent))

        // Expense set by Absolute Calendar Year
        val carLoan = com.muandrew.forecast.model.ExpenseItem(
            id = "loan1",
            name = "Auto Loan",
            category = ExpenseCategory.HOUSING_MORTGAGE,
            annualAmount = Money.ofDollars(6_000),
            entityId = parent.id,
            timeMode = com.muandrew.forecast.model.TimeMode.CALENDAR_YEAR,
            startYear = 2026,
            endYear = 2030
        )
        assertEquals(2026, carLoan.effectiveStartYear(parent))
        assertEquals(2030, carLoan.effectiveEndYear(parent))
        assertTrue(carLoan.isApplicableInYear(2028, parent))
        assertTrue(!carLoan.isApplicableInYear(2032, parent))

        // Child College Expense set by Child Entity Age (Ages 18-21)
        val college = com.muandrew.forecast.model.ExpenseItem(
            id = "tuition",
            name = "College Tuition",
            category = ExpenseCategory.EDUCATION_TUITION,
            annualAmount = Money.ofDollars(40_000),
            entityId = child.id,
            timeMode = com.muandrew.forecast.model.TimeMode.ENTITY_AGE,
            startAge = 18,
            endAge = 21
        )
        assertEquals(2046, college.effectiveStartYear(child)) // 2028 + 18 = 2046
        assertEquals(2049, college.effectiveEndYear(child)) // 2028 + 21 = 2049
        assertTrue(college.isApplicableInYear(2047, child))
        assertTrue(!college.isApplicableInYear(2030, child))

        // Asset Pool with Owner Entity
        val pool529 = AssetPool(
            id = "529",
            name = "Child 529",
            category = AssetCategory.TAXABLE_BROKERAGE,
            currentBalance = Money.ofDollars(10_000),
            entityId = child.id,
            annualContribution = Money.ofDollars(5_000),
            contributionEndAge = 18
        )
        assertEquals(child.id, pool529.entityId)
    }

    @Test
    fun testCashflowWaterfallPriorityOrder() {
        val parent = com.muandrew.forecast.model.Entity(id = "p1", name = "Alex", birthYear = 1996, isPrimary = true, retirementAge = 60, lifeExpectancy = 70)
        val child = com.muandrew.forecast.model.Entity(id = "c1", name = "Emma", birthYear = 2028, isPrimary = false)

        // Limited income of $50,000
        val income = com.muandrew.forecast.model.IncomeStream(
            id = "inc1",
            name = "Base Income",
            entityId = parent.id,
            initialAnnualAmount = Money.ofDollars(50_000),
            timeMode = com.muandrew.forecast.model.TimeMode.CALENDAR_YEAR,
            startYear = 2026,
            endYear = 2030,
            yearlyPayBumpRate = 0.0
        )

        // Debt of $20,000 (Priority 1)
        val debt = com.muandrew.forecast.model.ExpenseItem(
            id = "debt1",
            name = "High Interest Loan",
            category = ExpenseCategory.HOUSING_MORTGAGE,
            annualAmount = Money.ofDollars(20_000),
            entityId = parent.id,
            timeMode = com.muandrew.forecast.model.TimeMode.CALENDAR_YEAR,
            startYear = 2026,
            endYear = 2030,
            expenseType = com.muandrew.forecast.model.ExpenseType.COMPOUNDING_DEBT
        )

        // Living Essentials of $25,000 (Priority 2)
        val living = com.muandrew.forecast.model.ExpenseItem(
            id = "living1",
            name = "Essentials",
            category = ExpenseCategory.LIVING_ESSENTIALS,
            annualAmount = Money.ofDollars(25_000),
            entityId = parent.id,
            timeMode = com.muandrew.forecast.model.TimeMode.CALENDAR_YEAR,
            startYear = 2026,
            endYear = 2030,
            expenseType = com.muandrew.forecast.model.ExpenseType.RECURRING
        )

        // Child 529 Plan target contribution of $10,000 (Priority 3) -> Income remaining is $5,000, so $5,000 is funded and $5,000 is shortfall!
        val pool529 = AssetPool(
            id = "pool529",
            name = "Emma 529",
            category = AssetCategory.TAXABLE_BROKERAGE,
            currentBalance = Money.ZERO,
            entityId = child.id,
            annualContribution = Money.ofDollars(10_000),
            contributionEndAge = 18
        )

        val household = Household(
            id = "h1",
            name = "Test Family",
            baseYear = 2026,
            entities = listOf(parent, child),
            incomeStreams = listOf(income),
            expenses = listOf(debt, living),
            assetPools = listOf(pool529)
        )

        val timeline = MultiAssetEngine.simulateHousehold(household, inflationRate = 0.0)
        val year0 = timeline.first()

        assertEquals(3, year0.itemFundings.size)

        // 1. Debt: fully funded ($20,000)
        val debtFunding = year0.itemFundings.first { it.id == "debt1" }
        assertEquals(Money.ofDollars(20_000), debtFunding.actualAmount)
        assertEquals(com.muandrew.forecast.model.FundingStatus.FULLY_FUNDED, debtFunding.status)

        // 2. Living: fully funded ($25,000)
        val livingFunding = year0.itemFundings.first { it.id == "living1" }
        assertEquals(Money.ofDollars(25_000), livingFunding.actualAmount)
        assertEquals(com.muandrew.forecast.model.FundingStatus.FULLY_FUNDED, livingFunding.status)

        // 3. Child 529: partially funded ($5,000 / $10,000 target, shortfall $5,000)
        val poolFunding = year0.itemFundings.first { it.id == "pool529" }
        assertEquals(Money.ofDollars(5_000), poolFunding.actualAmount)
        assertEquals(Money.ofDollars(5_000), poolFunding.shortfall)
        assertEquals(com.muandrew.forecast.model.FundingStatus.PARTIALLY_FUNDED, poolFunding.status)
        assertTrue(poolFunding.status.isProblem) // Flagged for red highlight!
    }

    @Test
    fun testMultiPhaseAssetPoolContributions() {
        val person = com.muandrew.forecast.model.Entity(id = "p1", name = "Alex", birthYear = 1996, isPrimary = true, retirementAge = 60, lifeExpectancy = 70)

        // Phased pool: $1,000/yr for ages 20-30, $2,000/yr for ages 31-40
        val pool = AssetPool(
            id = "pool_phased",
            name = "Tiered Brokerage",
            category = AssetCategory.TAXABLE_BROKERAGE,
            currentBalance = Money.ZERO,
            entityId = person.id,
            annualContribution = Money.ZERO,
            phases = listOf(
                com.muandrew.forecast.model.SchedulePhase(
                    id = "phase1",
                    name = "Early Career",
                    timeMode = com.muandrew.forecast.model.TimeMode.ENTITY_AGE,
                    startAge = 20,
                    endAge = 30,
                    amount = Money.ofDollars(1_000)
                ),
                com.muandrew.forecast.model.SchedulePhase(
                    id = "phase2",
                    name = "Peak Career",
                    timeMode = com.muandrew.forecast.model.TimeMode.ENTITY_AGE,
                    startAge = 31,
                    endAge = 40,
                    amount = Money.ofDollars(2_000)
                )
            )
        )

        // Age 30 (Year 2026): $1,000
        assertEquals(Money.ofDollars(1_000), pool.targetContributionInYear(2026, person))

        // Age 31 (Year 2027): $2,000
        assertEquals(Money.ofDollars(2_000), pool.targetContributionInYear(2027, person))

        // Age 35 (Year 2031): $2,000
        assertEquals(Money.ofDollars(2_000), pool.targetContributionInYear(2031, person))

        // Age 50 (Year 2046, past endAge): $0
        assertEquals(Money.ZERO, pool.targetContributionInYear(2046, person))
    }

    @Test
    fun testAssetPoolWithdrawals() {
        val child = com.muandrew.forecast.model.Entity(id = "c1", name = "Emma", birthYear = 2028, isPrimary = false)

        val pool529 = AssetPool(
            id = "529",
            name = "Emma 529",
            category = AssetCategory.TAXABLE_BROKERAGE,
            currentBalance = Money.ofDollars(10_000),
            entityId = child.id,
            annualContribution = Money.ofDollars(5_000),
            phases = listOf(
                com.muandrew.forecast.model.SchedulePhase(
                    id = "save",
                    name = "Ages 0-17 Contributions",
                    timeMode = com.muandrew.forecast.model.TimeMode.ENTITY_AGE,
                    startAge = 0,
                    endAge = 17,
                    amount = Money.ofDollars(5_000),
                    isWithdrawal = false
                ),
                com.muandrew.forecast.model.SchedulePhase(
                    id = "draw",
                    name = "Ages 18-21 College Drawdown",
                    timeMode = com.muandrew.forecast.model.TimeMode.ENTITY_AGE,
                    startAge = 18,
                    endAge = 21,
                    amount = Money.ofDollars(20_000),
                    isWithdrawal = true
                )
            )
        )

        // Age 10 (Year 2038): $5,000 contribution (green bar), $0 withdrawal
        assertEquals(Money.ofDollars(5_000), pool529.targetContributionInYear(2038, child))
        assertEquals(Money.ZERO, pool529.targetWithdrawalInYear(2038, child))

        // Age 19 (Year 2047): $0 contribution, $20,000 withdrawal (red bar!)
        assertEquals(Money.ZERO, pool529.targetContributionInYear(2047, child))
        assertEquals(Money.ofDollars(20_000), pool529.targetWithdrawalInYear(2047, child))
    }

    @Test
    fun testAssetPoolOverWithdrawalDeficit() {
        val person = com.muandrew.forecast.model.Entity(id = "p1", name = "Test", birthYear = 1996, isPrimary = true)

        // Starting with $10k, attempting to withdraw $30k
        val smallPool = AssetPool(
            id = "p_small",
            name = "Small Brokerage",
            category = AssetCategory.TAXABLE_BROKERAGE,
            currentBalance = Money.ofDollars(10_000),
            entityId = person.id,
            phases = listOf(
                com.muandrew.forecast.model.SchedulePhase(
                    id = "draw",
                    name = "Large Drawdown",
                    timeMode = com.muandrew.forecast.model.TimeMode.CALENDAR_YEAR,
                    startYear = 2026,
                    endYear = 2028,
                    amount = Money.ofDollars(30_000),
                    isWithdrawal = true
                )
            )
        )

        var runningBalance = smallPool.currentBalance.value
        val startYr = 2026
        val yrPoints = (startYr..2028).map { yr ->
            val contrib = smallPool.targetContributionInYear(yr, person)
            val withdr = smallPool.targetWithdrawalInYear(yr, person)
            val growth = if (runningBalance > 0L) (runningBalance * 0.05).toLong() else 0L

            val netBeforeWithdrawal = runningBalance + growth + contrib.value
            val isDeficit = withdr.value > 0L && netBeforeWithdrawal < withdr.value
            val shortfallCents = if (isDeficit) withdr.value - netBeforeWithdrawal else 0L

            runningBalance = maxOf(0L, netBeforeWithdrawal - withdr.value)

            com.muandrew.forecast.ui.YearTrajectoryPoint(
                calendarYear = yr,
                age = person.ageInYear(yr),
                balance = Money(runningBalance),
                inflow = contrib,
                outflow = withdr,
                isDeficit = isDeficit || (runningBalance == 0L && withdr.value > 0L),
                shortfall = Money(shortfallCents)
            )
        }

        // Year 1 (2026): Net before = $10,500, Withdrawal = $30,000 -> Deficit is True, Shortfall = $19,500, Balance = $0
        assertTrue(yrPoints[0].isDeficit)
        assertEquals(Money.ZERO, yrPoints[0].balance)
        assertEquals(Money.ofDollars(19_500), yrPoints[0].shortfall)

        // Year 2 (2027): Net before = $0, Withdrawal = $30,000 -> Deficit is True, Shortfall = $30,000, Balance = $0
        assertTrue(yrPoints[1].isDeficit)
        assertEquals(Money.ZERO, yrPoints[1].balance)
        assertEquals(Money.ofDollars(30_000), yrPoints[1].shortfall)
    }

    @Test
    fun testExpenseItemTypes() {
        // 1. Recurring Expense
        val recurring = com.muandrew.forecast.model.ExpenseItem(
            id = "rec1",
            name = "Vacation",
            category = ExpenseCategory.DISCRETIONARY_VACATION,
            expenseType = com.muandrew.forecast.model.ExpenseType.RECURRING,
            annualAmount = Money.ofDollars(10_000),
            startAge = 30,
            endAge = 40,
            inflationAdjusted = false
        )
        assertEquals(Money.ofDollars(10_000), recurring.amountAtAge(35, baseAge = 30, inflationRate = 0.0))
        assertEquals(Money.ZERO, recurring.amountAtAge(45, baseAge = 30, inflationRate = 0.0))

        // 2. One-Time Lump Sum
        val oneTime = com.muandrew.forecast.model.ExpenseItem(
            id = "ot1",
            name = "Down Payment",
            category = ExpenseCategory.HOUSING_MORTGAGE,
            expenseType = com.muandrew.forecast.model.ExpenseType.ONE_TIME,
            annualAmount = Money.ofDollars(80_000),
            startAge = 33,
            endAge = 33,
            inflationAdjusted = false
        )
        assertEquals(Money.ofDollars(80_000), oneTime.amountAtAge(33, baseAge = 30, inflationRate = 0.0))
        assertEquals(Money.ZERO, oneTime.amountAtAge(34, baseAge = 30, inflationRate = 0.0))

        // 3. Compounding Debt (e.g. 10% APR interest compounding)
        val debt = com.muandrew.forecast.model.ExpenseItem(
            id = "d1",
            name = "Debt Payment",
            category = ExpenseCategory.HOUSING_MORTGAGE,
            expenseType = com.muandrew.forecast.model.ExpenseType.COMPOUNDING_DEBT,
            annualAmount = Money.ofDollars(5_000),
            startAge = 30,
            endAge = 35,
            compoundingInterestRate = 0.10,
            inflationAdjusted = false
        )
        assertEquals(Money.ofDollars(5_000), debt.amountAtAge(30, baseAge = 30, inflationRate = 0.0))
        assertEquals(Money.ofDollars(5_500), debt.amountAtAge(31, baseAge = 30, inflationRate = 0.0))
        assertEquals(Money.ofDollars(6_050), debt.amountAtAge(32, baseAge = 30, inflationRate = 0.0))
    }

    @Test
    fun testSWRCalculation() {
        val profile = ForecastProfile(
            currentAge = 30,
            retirementAge = 60,
            annualRetirementExpenses = Money.ofDollars(40_000)
        )
        val swr = RetirementCalculator.calculateSWR(profile, swrRate = 0.04)

        // $40,000 / 0.04 = $1,000,000 target
        assertEquals(Money.ofDollars(1_000_000), swr.targetPortfolioSize)
    }

    @Test
    fun testChildTemplateStages() {
        val child = ChildTemplate(
            id = "c1",
            childName = "Emma",
            parentAgeAtBirth = 30,
            annualDaycareCost = Money.ofDollars(20_000),
            annualSchoolAgeLivingCost = Money.ofDollars(10_000),
            annual529Contribution = Money.ofDollars(5_000),
            annualCollegeTuition = Money.ofDollars(40_000)
        )

        val expenses = child.generateExpenses(parentCurrentAge = 28)
        assertEquals(4, expenses.size)

        // Daycare stage (ages 30-35)
        val daycare = expenses.first { it.category == ExpenseCategory.CHILDCARE_EARLY }
        assertEquals(30, daycare.startAge)
        assertEquals(35, daycare.endAge)
        assertEquals(Money.ofDollars(20_000), daycare.annualAmount)

        // College stage (ages 48-51)
        val college = expenses.first { it.category == ExpenseCategory.EDUCATION_TUITION && it.annualAmount == Money.ofDollars(40_000) }
        assertEquals(48, college.startAge)
        assertEquals(51, college.endAge)
    }

    @Test
    fun testHomePurchaseTemplate() {
        val home = HomePurchaseTemplate(
            id = "h1",
            propertyName = "Suburban Home",
            parentAgeAtPurchase = 35,
            propertyValue = Money.ofDollars(500_000),
            downPaymentPercent = 0.20
        )

        val expenses = home.generateExpenses(30)
        val downPayment = expenses.first { it.name.contains("Down Payment") }
        assertEquals(Money.ofDollars(100_000), downPayment.annualAmount)
        assertEquals(35, downPayment.startAge)

        val assets = home.generateAssetPools()
        assertEquals(1, assets.size)
        assertEquals(Money.ofDollars(500_000), assets.first().currentBalance)
        assertEquals(AssetCategory.REAL_ESTATE, assets.first().category)
    }

    @Test
    fun testMultiAssetEngineSimulation() {
        val entity = com.muandrew.forecast.model.Entity(id = "p_main", name = "Primary", birthYear = 1996, isPrimary = true, retirementAge = 60, lifeExpectancy = 80)
        val household = Household(
            id = "h_main",
            name = "Primary Family",
            baseYear = 2026,
            entities = listOf(entity),
            annualIncome = Money.ofDollars(100_000),
            assetPools = listOf(
                AssetPool("p_tax", "Brokerage", AssetCategory.TAXABLE_BROKERAGE, Money.ofDollars(40_000), annualContribution = Money.ofDollars(10_000)),
                AssetPool("p_401", "401k", AssetCategory.PRE_TAX_401K, Money.ofDollars(60_000), annualContribution = Money.ofDollars(15_000))
            ),
            directExpenses = listOf(
                ExpenseItem("e_living", "Living", ExpenseCategory.LIVING_ESSENTIALS, Money.ofDollars(50_000), startAge = 30, endAge = 80)
            )
        )

        val timeline = MultiAssetEngine.simulateHousehold(household, inflationRate = 0.02)
        assertEquals(51, timeline.size) // ages 30 to 80 inclusive

        // Age 30 breakdown
        val initialYear = timeline.first()
        assertEquals(30, initialYear.age)
        assertEquals(Money.ofDollars(100_000), initialYear.totalNetWorth)
        assertEquals(Money.ofDollars(40_000), initialYear.assetBalances[AssetCategory.TAXABLE_BROKERAGE])
        assertEquals(Money.ofDollars(60_000), initialYear.assetBalances[AssetCategory.PRE_TAX_401K])

        // Verify wealth accumulation over working years
        val retirementYear = timeline.first { it.age == 60 }
        assertTrue(retirementYear.totalNetWorth.value > initialYear.totalNetWorth.value)
    }

    @Test
    fun testMultiHouseholdPlanConsolidation() {
        val h1 = Household(
            id = "h1",
            name = "Primary",
            baseYear = 2026,
            entities = listOf(com.muandrew.forecast.model.Entity("e1", "Primary", birthYear = 1996, isPrimary = true, retirementAge = 60, lifeExpectancy = 70)),
            assetPools = listOf(AssetPool("p1", "Cash", AssetCategory.CASH_EMERGENCY, Money.ofDollars(50_000)))
        )

        val h2 = Household(
            id = "h2",
            name = "Parents",
            baseYear = 2026,
            entities = listOf(com.muandrew.forecast.model.Entity("e2", "Parents", birthYear = 1996, isPrimary = true, retirementAge = 60, lifeExpectancy = 70)),
            assetPools = listOf(AssetPool("p2", "IRA", AssetCategory.PRE_TAX_401K, Money.ofDollars(100_000)))
        )

        val plan = FinancialPlan(households = listOf(h1, h2))
        val result = MultiAssetEngine.simulatePlan(plan, simulationsCount = 50)

        assertEquals(2, result.householdResults.size)
        val initialConsolidated = result.timeline.first()
        assertEquals(Money.ofDollars(150_000), initialConsolidated.totalNetWorth)
        assertEquals(Money.ofDollars(50_000), initialConsolidated.assetBalances[AssetCategory.CASH_EMERGENCY])
        assertEquals(Money.ofDollars(100_000), initialConsolidated.assetBalances[AssetCategory.PRE_TAX_401K])
    }
}
