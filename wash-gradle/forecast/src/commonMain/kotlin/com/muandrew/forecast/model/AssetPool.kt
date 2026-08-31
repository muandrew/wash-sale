package com.muandrew.forecast.model

enum class AssetCategory(
    val displayName: String,
    val hexColor: Long,
    val defaultReturnRate: Double,
    val defaultVolatility: Double
) {
    TAXABLE_BROKERAGE("Taxable Brokerage", 0xFF42A5F5, 0.075, 0.16),
    PRE_TAX_401K("401(k) / Traditional IRA", 0xFF66BB6A, 0.070, 0.14),
    ROTH_IRA("Roth IRA / HSA", 0xFFAB47BC, 0.075, 0.15),
    CASH_EMERGENCY("High-Yield Cash", 0xFFFFCA28, 0.035, 0.02),
    REAL_ESTATE("Real Estate Property", 0xFFFF7043, 0.050, 0.08),
    OTHER("Other Assets", 0xFF78909C, 0.050, 0.10)
}

data class AssetPool(
    val id: String,
    val name: String,
    val category: AssetCategory,
    val currentBalance: Money,
    val entityId: String = "",
    val expectedNominalReturn: Double = category.defaultReturnRate,
    val returnVolatility: Double = category.defaultVolatility,
    val annualContribution: Money = Money.ZERO,
    val contributionEndAge: Int? = null,
    val annualWithdrawal: Money = Money.ZERO,
    val withdrawalStartAge: Int? = null,
    val withdrawalEndAge: Int? = null,
    val phases: List<SchedulePhase> = emptyList()
) {
    /**
     * Calculates the target annual contribution (inflow) in a specific calendar year.
     */
    fun targetContributionInYear(calendarYear: Int, owner: Entity?): Money {
        val contribPhases = phases.filter { !it.isWithdrawal }
        if (contribPhases.isNotEmpty()) {
            val matchingPhase = contribPhases.firstOrNull { it.isApplicableInYear(calendarYear, owner) }
            return matchingPhase?.amount ?: Money.ZERO
        }
        val ownerAge = owner?.ageInYear(calendarYear) ?: 30
        val endAge = contributionEndAge ?: owner?.retirementAge ?: 65
        return if (ownerAge < endAge) annualContribution else Money.ZERO
    }

    /**
     * Calculates the target annual withdrawal (outflow) in a specific calendar year.
     */
    fun targetWithdrawalInYear(calendarYear: Int, owner: Entity?): Money {
        val withdrPhases = phases.filter { it.isWithdrawal }
        if (withdrPhases.isNotEmpty()) {
            val matchingPhase = withdrPhases.firstOrNull { it.isApplicableInYear(calendarYear, owner) }
            return matchingPhase?.amount ?: Money.ZERO
        }
        if (annualWithdrawal.value > 0L) {
            val ownerAge = owner?.ageInYear(calendarYear) ?: 30
            val startAge = withdrawalStartAge ?: contributionEndAge ?: owner?.retirementAge ?: 60
            val endAge = withdrawalEndAge ?: 95
            if (ownerAge in startAge..endAge) {
                return annualWithdrawal
            }
        }
        return Money.ZERO
    }
}
