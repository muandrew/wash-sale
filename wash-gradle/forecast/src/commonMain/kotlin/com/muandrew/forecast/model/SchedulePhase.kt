package com.muandrew.forecast.model

data class SchedulePhase(
    val id: String = "",
    val name: String = "",
    val timeMode: TimeMode = TimeMode.ENTITY_AGE,
    val startAge: Int = 30,
    val endAge: Int = 90,
    val startYear: Int = 2026,
    val endYear: Int = 2086,
    val amount: Money = Money.ZERO,
    val isWithdrawal: Boolean = false,
) {
    fun effectiveStartYear(entity: Entity?): Int = when (timeMode) {
        TimeMode.CALENDAR_YEAR -> startYear
        TimeMode.ENTITY_AGE -> if (entity != null) entity.yearAtAge(startAge) else startYear
    }

    fun effectiveEndYear(entity: Entity?): Int = when (timeMode) {
        TimeMode.CALENDAR_YEAR -> endYear
        TimeMode.ENTITY_AGE -> if (entity != null) entity.yearAtAge(endAge) else endYear
    }

    fun isApplicableInYear(
        calendarYear: Int,
        entity: Entity?,
    ): Boolean {
        val sYear = effectiveStartYear(entity)
        val eYear = effectiveEndYear(entity)
        return calendarYear in sYear..eYear
    }
}

/**
 * Timeline attribute override for Asset Pools.
 * Row 1 sets all baseline attributes. Subsequent rows only specify starting time and attributes to override.
 * annualFlow: Signed Money (+ for Deposit/Contribution, - for Drawdown/Withdrawal).
 */
data class AssetPoolOverride(
    val id: String = "",
    val timeMode: TimeMode = TimeMode.ENTITY_AGE,
    val startAge: Int = 30,
    val startYear: Int = 2026,
    val expectedNominalReturn: Double? = null,
    val annualFlow: Money? = null, // Signed: + for Deposit, - for Drawdown
    val annualContribution: Money? = null, // Backward-compat fallback
    val annualWithdrawal: Money? = null, // Backward-compat fallback
    val label: String = "",
) {
    fun effectiveFlow(): Money? {
        if (annualFlow != null) return annualFlow
        if (annualWithdrawal != null && annualWithdrawal.value > 0L) return Money(-annualWithdrawal.value)
        if (annualContribution != null) return annualContribution
        return null
    }

    fun effectiveStartYear(entity: Entity?): Int = when (timeMode) {
        TimeMode.CALENDAR_YEAR -> startYear
        TimeMode.ENTITY_AGE -> if (entity != null) entity.yearAtAge(startAge) else startYear
    }
}

/**
 * Timeline attribute override for Income Streams.
 * Row 1 sets baseline pay & yearly raise bump. Subsequent rows override salary or bump rate at milestone times.
 */
data class IncomeStreamOverride(
    val id: String = "",
    val timeMode: TimeMode = TimeMode.ENTITY_AGE,
    val startAge: Int = 30,
    val startYear: Int = 2026,
    val annualAmount: Money? = null,
    val yearlyPayBumpRate: Double? = null,
    val label: String = "",
) {
    fun effectiveStartYear(entity: Entity?): Int = when (timeMode) {
        TimeMode.CALENDAR_YEAR -> startYear
        TimeMode.ENTITY_AGE -> if (entity != null) entity.yearAtAge(startAge) else startYear
    }
}

/**
 * Timeline attribute override for Expenses & Loans.
 * Row 1 sets baseline amount, type, interest. Subsequent rows override amount, type, or interest at milestone times.
 */
data class ExpenseItemOverride(
    val id: String = "",
    val timeMode: TimeMode = TimeMode.ENTITY_AGE,
    val startAge: Int = 30,
    val startYear: Int = 2026,
    val annualAmount: Money? = null,
    val expenseType: ExpenseType? = null,
    val compoundingInterestRate: Double? = null,
    val label: String = "",
) {
    fun effectiveStartYear(entity: Entity?): Int = when (timeMode) {
        TimeMode.CALENDAR_YEAR -> startYear
        TimeMode.ENTITY_AGE -> if (entity != null) entity.yearAtAge(startAge) else startYear
    }
}
