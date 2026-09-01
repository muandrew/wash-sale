package com.muandrew.forecast.model

import kotlin.math.pow

enum class ExpenseType(val displayName: String) {
    RECURRING("Recurring Annual"),
    ONE_TIME("One-Time Lump Sum"),
    COMPOUNDING_DEBT("Compounding Debt / Loan")
}

enum class ExpenseCategory(
    val displayName: String,
    val hexColor: Long
) {
    LIVING_ESSENTIALS("Living & Essential Needs", 0xFFEF5350),
    DISCRETIONARY_VACATION("Discretionary & Vacation", 0xFF26A69A),
    EDUCATION_TUITION("Education & College Tuition", 0xFF5C6BC0),
    CHILDCARE_EARLY("Early Childcare & Daycare", 0xFFFFA726),
    HOUSING_MORTGAGE("Housing, Mortgage & Debt", 0xFF8D6E63),
    HEALTHCARE("Healthcare & Medical", 0xFFEC407A),
    MILESTONE_OTHER("Milestone & Other Purchases", 0xFF7E57C2)
}

data class ExpenseItem(
    val id: String,
    val name: String,
    val category: ExpenseCategory,
    val annualAmount: Money,
    val entityId: String = "",
    val timeMode: TimeMode = TimeMode.ENTITY_AGE,
    val startAge: Int = 30,
    val endAge: Int = 90,
    val startYear: Int = 2026,
    val endYear: Int = 2086,
    val expenseType: ExpenseType = ExpenseType.RECURRING,
    val compoundingInterestRate: Double = 0.0, // e.g. 0.07 for 7.0% APR compounding debt
    val inflationAdjusted: Boolean = true,
    val overrides: List<ExpenseItemOverride> = emptyList(),
    val phases: List<SchedulePhase> = emptyList()
) {
    fun effectiveStartYear(entity: Entity?): Int {
        return when (timeMode) {
            TimeMode.CALENDAR_YEAR -> startYear
            TimeMode.ENTITY_AGE -> if (entity != null) entity.yearAtAge(startAge) else startYear
        }
    }

    fun effectiveEndYear(entity: Entity?): Int {
        return when (timeMode) {
            TimeMode.CALENDAR_YEAR -> if (expenseType == ExpenseType.ONE_TIME) startYear else endYear
            TimeMode.ENTITY_AGE -> if (entity != null) {
                if (expenseType == ExpenseType.ONE_TIME) entity.yearAtAge(startAge) else entity.yearAtAge(endAge)
            } else {
                if (expenseType == ExpenseType.ONE_TIME) startYear else endYear
            }
        }
    }

    fun isApplicableInYear(calendarYear: Int, entity: Entity?): Boolean {
        if (phases.isNotEmpty() && overrides.isEmpty()) {
            return phases.any { it.isApplicableInYear(calendarYear, entity) }
        }
        val sYear = effectiveStartYear(entity)
        if (calendarYear < sYear) return false
        if (overrides.isEmpty()) {
            val eYear = effectiveEndYear(entity)
            return calendarYear in sYear..eYear
        }
        return true
    }

    fun amountInYear(calendarYear: Int, baseYear: Int, entity: Entity?, inflationRate: Double): Money {
        if (phases.isNotEmpty() && overrides.isEmpty()) {
            val matchingPhase = phases.firstOrNull { it.isApplicableInYear(calendarYear, entity) } ?: return Money.ZERO
            val sYear = matchingPhase.effectiveStartYear(entity)
            val yearsFromBase = (calendarYear - baseYear).coerceAtLeast(0)
            val infFactor = if (inflationAdjusted) (1.0 + inflationRate).pow(yearsFromBase.toDouble()) else 1.0

            return when (expenseType) {
                ExpenseType.ONE_TIME -> {
                    if (calendarYear == sYear) Money((matchingPhase.amount.value * infFactor).toLong()) else Money.ZERO
                }
                ExpenseType.RECURRING -> {
                    Money((matchingPhase.amount.value * infFactor).toLong())
                }
                ExpenseType.COMPOUNDING_DEBT -> {
                    val yearsFromStart = (calendarYear - sYear).coerceAtLeast(0)
                    val interestFactor = (1.0 + compoundingInterestRate).pow(yearsFromStart.toDouble())
                    Money((matchingPhase.amount.value * interestFactor * infFactor).toLong())
                }
            }
        }

        val baseStartYear = effectiveStartYear(entity)
        if (calendarYear < baseStartYear) return Money.ZERO

        if (overrides.isEmpty()) {
            val baseEndYear = effectiveEndYear(entity)
            if (calendarYear > baseEndYear) return Money.ZERO
        }

        var activeStartYear = baseStartYear
        var activeAmount = annualAmount
        var activeType = expenseType
        var activeInterest = compoundingInterestRate

        val sortedOverrides = overrides.sortedBy { it.effectiveStartYear(entity) }
        for (ov in sortedOverrides) {
            val ovStart = ov.effectiveStartYear(entity)
            if (ovStart <= calendarYear) {
                activeStartYear = ovStart
                ov.annualAmount?.let { activeAmount = it }
                ov.expenseType?.let { activeType = it }
                ov.compoundingInterestRate?.let { activeInterest = it }
            }
        }

        if (activeAmount.value <= 0L) return Money.ZERO

        val yearsFromBase = (calendarYear - baseYear).coerceAtLeast(0)
        val infFactor = if (inflationAdjusted) (1.0 + inflationRate).pow(yearsFromBase.toDouble()) else 1.0

        return when (activeType) {
            ExpenseType.ONE_TIME -> {
                if (calendarYear == activeStartYear) Money((activeAmount.value * infFactor).toLong()) else Money.ZERO
            }
            ExpenseType.RECURRING -> {
                Money((activeAmount.value * infFactor).toLong())
            }
            ExpenseType.COMPOUNDING_DEBT -> {
                val yearsFromStart = (calendarYear - activeStartYear).coerceAtLeast(0)
                val interestFactor = (1.0 + activeInterest).pow(yearsFromStart.toDouble())
                Money((activeAmount.value * interestFactor * infFactor).toLong())
            }
        }
    }

    // Compatibility methods
    fun isApplicableAtAge(age: Int): Boolean = age in startAge..endAge

    fun amountAtAge(age: Int, baseAge: Int, inflationRate: Double): Money {
        if (!isApplicableAtAge(age)) return Money.ZERO

        return when (expenseType) {
            ExpenseType.ONE_TIME -> {
                if (age == startAge) {
                    val years = (age - baseAge).coerceAtLeast(0)
                    val factor = if (inflationAdjusted) (1.0 + inflationRate).pow(years.toDouble()) else 1.0
                    Money((annualAmount.value * factor).toLong())
                } else {
                    Money.ZERO
                }
            }
            ExpenseType.RECURRING -> {
                val years = (age - baseAge).coerceAtLeast(0)
                val factor = if (inflationAdjusted) (1.0 + inflationRate).pow(years.toDouble()) else 1.0
                Money((annualAmount.value * factor).toLong())
            }
            ExpenseType.COMPOUNDING_DEBT -> {
                val yearsFromStart = (age - startAge).coerceAtLeast(0)
                val interestFactor = (1.0 + compoundingInterestRate).pow(yearsFromStart.toDouble())
                val yearsFromBase = (age - baseAge).coerceAtLeast(0)
                val infFactor = if (inflationAdjusted) (1.0 + inflationRate).pow(yearsFromBase.toDouble()) else 1.0
                Money((annualAmount.value * interestFactor * infFactor).toLong())
            }
        }
    }
}
