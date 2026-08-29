package com.muandrew.forecast.model

data class ForecastProfile(
    val currentAge: Int = 30,
    val retirementAge: Int = 60,
    val lifeExpectancy: Int = 90,
    val currentNetWorth: Money = Money.ofDollars(100_000),
    val annualSavings: Money = Money.ofDollars(25_000),
    val annualRetirementExpenses: Money = Money.ofDollars(60_000),
    val expectedReturnRate: Double = 0.07, // 7% nominal return
    val inflationRate: Double = 0.025,     // 2.5% inflation
    val returnVolatility: Double = 0.15    // 15% standard deviation
)

data class YearTrajectory(
    val yearIndex: Int,
    val age: Int,
    val balanceP10: Money,
    val balanceP25: Money,
    val balanceP50: Money,
    val balanceP75: Money,
    val balanceP90: Money
)

data class MonteCarloResult(
    val totalSimulations: Int,
    val successfulSimulations: Int,
    val successRate: Double,
    val trajectory: List<YearTrajectory>,
    val finalMedianNetWorth: Money,
    val p10FinalNetWorth: Money,
    val p90FinalNetWorth: Money
)

data class SWRAnalysis(
    val safeWithdrawalRate: Double, // e.g. 0.04 (4%)
    val annualExpenseTarget: Money,
    val targetPortfolioSize: Money,
    val projectedPortfolioAtRetirement: Money,
    val fundingRatio: Double,
    val isRetirementFunded: Boolean
)
