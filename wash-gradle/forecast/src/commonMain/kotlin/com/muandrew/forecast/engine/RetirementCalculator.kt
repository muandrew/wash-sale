package com.muandrew.forecast.engine

import com.muandrew.forecast.model.ForecastProfile
import com.muandrew.forecast.model.Money
import com.muandrew.forecast.model.SWRAnalysis
import kotlin.math.max

object RetirementCalculator {

    /**
     * Computes Safe Withdrawal Rate (SWR) targets and retirement readiness.
     */
    fun calculateSWR(
        profile: ForecastProfile,
        swrRate: Double = 0.04, // 4% rule
    ): SWRAnalysis {
        require(swrRate > 0.0) { "SWR rate must be positive." }

        val targetPortfolioCents = (profile.annualRetirementExpenses.value / swrRate).toLong()
        val targetPortfolio = Money(targetPortfolioCents)

        val workingYears = max(0, profile.retirementAge - profile.currentAge)
        val realReturnRate = (1.0 + profile.expectedReturnRate) / (1.0 + profile.inflationRate) - 1.0

        var balance = profile.currentNetWorth.value.toDouble()
        for (year in 1..workingYears) {
            balance *= (1.0 + realReturnRate)
            balance += profile.annualSavings.value.toDouble()
        }

        val projectedAtRetirement = Money(balance.toLong())
        val fundingRatio = if (targetPortfolio.value > 0) {
            projectedAtRetirement.value.toDouble() / targetPortfolio.value.toDouble()
        } else {
            1.0
        }

        return SWRAnalysis(
            safeWithdrawalRate = swrRate,
            annualExpenseTarget = profile.annualRetirementExpenses,
            targetPortfolioSize = targetPortfolio,
            projectedPortfolioAtRetirement = projectedAtRetirement,
            fundingRatio = fundingRatio,
            isRetirementFunded = fundingRatio >= 1.0,
        )
    }

    /**
     * Calculates Guyton-Klinger dynamic withdrawal boundaries for a portfolio.
     */
    data class GuytonKlingerBounds(
        val baselineWithdrawal: Money,
        val upperCapitalPreservationThreshold: Money,
        val lowerProsperityThreshold: Money,
    )

    fun calculateGuytonKlingerBounds(
        portfolio: Money,
        targetRate: Double = 0.04,
    ): GuytonKlingerBounds {
        val baseline = Money((portfolio.value * targetRate).toLong())
        // Capital preservation rule: trigger cut if withdrawal rate exceeds target by 20%
        val upperTrigger = Money((portfolio.value * (targetRate * 1.2)).toLong())
        // Prosperity rule: trigger increase if withdrawal rate falls 20% below target
        val lowerTrigger = Money((portfolio.value * (targetRate * 0.8)).toLong())

        return GuytonKlingerBounds(
            baselineWithdrawal = baseline,
            upperCapitalPreservationThreshold = upperTrigger,
            lowerProsperityThreshold = lowerTrigger,
        )
    }
}
