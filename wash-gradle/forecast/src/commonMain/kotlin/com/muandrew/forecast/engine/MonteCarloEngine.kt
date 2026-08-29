package com.muandrew.forecast.engine

import com.muandrew.forecast.model.ForecastProfile
import com.muandrew.forecast.model.Money
import com.muandrew.forecast.model.MonteCarloResult
import com.muandrew.forecast.model.YearTrajectory
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

object MonteCarloEngine {

    /**
     * Runs stochastic Monte Carlo simulations based on Box-Muller Gaussian sampling.
     */
    fun runSimulation(
        profile: ForecastProfile,
        simulationsCount: Int = 1000,
        randomSeed: Long? = null
    ): MonteCarloResult {
        val random = if (randomSeed != null) Random(randomSeed) else Random.Default
        val totalYears = max(1, profile.lifeExpectancy - profile.currentAge)
        val workingYears = max(0, profile.retirementAge - profile.currentAge)

        // Matrix of balances: simulationsCount x (totalYears + 1)
        val allPaths = Array(simulationsCount) {
            LongArray(totalYears + 1)
        }

        var successfulRuns = 0

        for (sim in 0 until simulationsCount) {
            var currentBalance = profile.currentNetWorth.value
            allPaths[sim][0] = currentBalance
            var failed = false

            for (year in 1..totalYears) {
                val isWorking = year <= workingYears

                // Generate normally distributed random return using Box-Muller
                val u1 = max(1e-10, random.nextDouble())
                val u2 = random.nextDouble()
                val z0 = sqrt(-2.0 * ln(u1)) * cos(2.0 * kotlin.math.PI * u2)

                // Annual rate of return: mean + z0 * standardDeviation
                val nominalReturn = profile.expectedReturnRate + z0 * profile.returnVolatility
                // Inflation-adjusted net real return rate
                val realReturn = (1.0 + nominalReturn) / (1.0 + profile.inflationRate) - 1.0

                if (currentBalance > 0) {
                    val growth = (currentBalance.toDouble() * realReturn).toLong()
                    currentBalance += growth
                }

                if (isWorking) {
                    currentBalance += profile.annualSavings.value
                } else {
                    currentBalance -= profile.annualRetirementExpenses.value
                }

                if (currentBalance <= 0) {
                    currentBalance = 0
                    failed = true
                }

                allPaths[sim][year] = currentBalance
            }

            if (!failed && currentBalance > 0) {
                successfulRuns++
            }
        }

        val trajectory = mutableListOf<YearTrajectory>()

        for (year in 0..totalYears) {
            val balancesForYear = LongArray(simulationsCount) { sim -> allPaths[sim][year] }
            balancesForYear.sort()

            fun percentile(p: Double): Long {
                val index = ((balancesForYear.size - 1) * p).toInt().coerceIn(0, balancesForYear.size - 1)
                return balancesForYear[index]
            }

            trajectory.add(
                YearTrajectory(
                    yearIndex = year,
                    age = profile.currentAge + year,
                    balanceP10 = Money(percentile(0.10)),
                    balanceP25 = Money(percentile(0.25)),
                    balanceP50 = Money(percentile(0.50)),
                    balanceP75 = Money(percentile(0.75)),
                    balanceP90 = Money(percentile(0.90))
                )
            )
        }

        val finalTrajectory = trajectory.last()

        return MonteCarloResult(
            totalSimulations = simulationsCount,
            successfulSimulations = successfulRuns,
            successRate = (successfulRuns.toDouble() / simulationsCount.toDouble()) * 100.0,
            trajectory = trajectory,
            finalMedianNetWorth = finalTrajectory.balanceP50,
            p10FinalNetWorth = finalTrajectory.balanceP10,
            p90FinalNetWorth = finalTrajectory.balanceP90
        )
    }
}
