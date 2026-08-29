package com.muandrew.forecast

import com.muandrew.forecast.engine.MonteCarloEngine
import com.muandrew.forecast.engine.RetirementCalculator
import com.muandrew.forecast.model.ForecastProfile
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
    fun testMonteCarloSimulation() {
        val profile = ForecastProfile(
            currentAge = 30,
            retirementAge = 60,
            lifeExpectancy = 80,
            currentNetWorth = Money.ofDollars(50_000),
            annualSavings = Money.ofDollars(20_000),
            annualRetirementExpenses = Money.ofDollars(40_000)
        )

        val result = MonteCarloEngine.runSimulation(profile, simulationsCount = 100, randomSeed = 1234L)
        assertEquals(100, result.totalSimulations)
        assertTrue(result.trajectory.isNotEmpty())

        for (traj in result.trajectory) {
            assertTrue(traj.balanceP10.value <= traj.balanceP25.value, "P10 <= P25 at age ${traj.age}")
            assertTrue(traj.balanceP25.value <= traj.balanceP50.value, "P25 <= P50 at age ${traj.age}")
            assertTrue(traj.balanceP50.value <= traj.balanceP75.value, "P50 <= P75 at age ${traj.age}")
            assertTrue(traj.balanceP75.value <= traj.balanceP90.value, "P75 <= P90 at age ${traj.age}")
        }
    }
}
