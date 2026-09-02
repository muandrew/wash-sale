package com.muandrew.forecast.model

enum class AssetCategory(
    val displayName: String,
    val hexColor: Long,
    val defaultReturnRate: Double,
    val defaultVolatility: Double,
) {
    TAXABLE_BROKERAGE("Taxable Brokerage", 0xFF42A5F5, 0.075, 0.16),
    PRE_TAX_401K("401(k) / Traditional IRA", 0xFF66BB6A, 0.070, 0.14),
    ROTH_IRA("Roth IRA / HSA", 0xFFAB47BC, 0.075, 0.15),
    CASH_EMERGENCY("High-Yield Cash", 0xFFFFCA28, 0.035, 0.02),
    REAL_ESTATE("Real Estate Property", 0xFFFF7043, 0.050, 0.08),
    OTHER("Other Assets", 0xFF78909C, 0.050, 0.10),
}

data class AssetPool(
    val id: String,
    val name: String,
    val category: AssetCategory,
    val currentBalance: Money,
    val entityId: String = "",
    val expectedNominalReturn: Double = category.defaultReturnRate,
    val returnVolatility: Double = category.defaultVolatility,
    val annualFlow: Money = Money.ZERO, // Signed Money: + for Deposit/Contribution, - for Drawdown/Withdrawal
    val annualContribution: Money = Money.ZERO,
    val contributionEndAge: Int? = null,
    val annualWithdrawal: Money = Money.ZERO,
    val withdrawalStartAge: Int? = null,
    val withdrawalEndAge: Int? = null,
    val startAge: Int = 30,
    val startYear: Int = 2026,
    val timeMode: TimeMode = TimeMode.ENTITY_AGE,
    val overrides: List<AssetPoolOverride> = emptyList(),
    val phases: List<SchedulePhase> = emptyList(),
) {
    fun baseFlow(): Money {
        if (annualFlow.value != 0L) return annualFlow
        if (annualWithdrawal.value > 0L) return Money(-annualWithdrawal.value)
        return annualContribution
    }

    /**
     * Calculates the effective return rate and signed flow (+ deposit, - drawdown) in a specific year.
     */
    fun effectiveFlowInYear(
        calendarYear: Int,
        owner: Entity?,
    ): Pair<Double, Money> {
        var rate = expectedNominalReturn
        var flow = baseFlow()

        // Legacy phases bridge if present and no overrides
        if (phases.isNotEmpty() && overrides.isEmpty()) {
            val contribPhases = phases.filter { !it.isWithdrawal }
            val matchingContrib = contribPhases.firstOrNull { it.isApplicableInYear(calendarYear, owner) }
            val contrib =
                matchingContrib?.amount
                    ?: (
                        if ((owner?.ageInYear(calendarYear) ?: 30) <
                            (contributionEndAge ?: owner?.retirementAge ?: 65)
                        ) {
                            annualContribution
                        } else {
                            Money.ZERO
                        }
                        )

            val withdrPhases = phases.filter { it.isWithdrawal }
            val matchingWithdr = withdrPhases.firstOrNull { it.isApplicableInYear(calendarYear, owner) }
            val withdr =
                matchingWithdr?.amount
                    ?: (
                        if ((owner?.ageInYear(calendarYear) ?: 30) >=
                            (withdrawalStartAge ?: contributionEndAge ?: owner?.retirementAge ?: 60)
                        ) {
                            annualWithdrawal
                        } else {
                            Money.ZERO
                        }
                        )

            val net = if (withdr.value > 0L) Money(-withdr.value) else contrib
            return Pair(rate, net)
        }

        // Chronologically evaluate overrides
        val sortedOverrides = overrides.sortedBy { it.effectiveStartYear(owner) }
        for (ov in sortedOverrides) {
            if (ov.effectiveStartYear(owner) <= calendarYear) {
                ov.expectedNominalReturn?.let { rate = it }
                ov.effectiveFlow()?.let { flow = it }
            }
        }
        return Pair(rate, flow)
    }

    /**
     * Calculates the effective return rate, contribution (deposit), and withdrawal in a specific year.
     */
    fun effectiveAttributesInYear(
        calendarYear: Int,
        owner: Entity?,
    ): Triple<Double, Money, Money> {
        val (rate, flow) = effectiveFlowInYear(calendarYear, owner)
        val contrib = if (flow.value > 0L) flow else Money.ZERO
        val withdr = if (flow.value < 0L) Money(-flow.value) else Money.ZERO
        return Triple(rate, contrib, withdr)
    }

    fun targetContributionInYear(
        calendarYear: Int,
        owner: Entity?,
    ): Money = effectiveAttributesInYear(calendarYear, owner).second

    fun targetWithdrawalInYear(
        calendarYear: Int,
        owner: Entity?,
    ): Money = effectiveAttributesInYear(calendarYear, owner).third

    fun expectedReturnInYear(
        calendarYear: Int,
        owner: Entity?,
    ): Double = effectiveAttributesInYear(calendarYear, owner).first
}
