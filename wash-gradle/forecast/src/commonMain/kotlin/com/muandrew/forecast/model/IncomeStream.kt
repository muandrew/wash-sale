package com.muandrew.forecast.model

import kotlin.math.pow

data class IncomeStream(
    val id: String,
    val name: String,
    val initialAnnualAmount: Money,
    val entityId: String = "",
    val timeMode: TimeMode = TimeMode.ENTITY_AGE,
    val startAge: Int = 30,
    val endAge: Int = 65,
    val startYear: Int = 2026,
    val endYear: Int = 2061,
    val yearlyPayBumpRate: Double = 0.03, // 3.0% yearly raise / merit growth
    val inflationAdjusted: Boolean = true,
    val overrides: List<IncomeStreamOverride> = emptyList(),
    val phases: List<SchedulePhase> = emptyList(),
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

    fun amountInYear(
        calendarYear: Int,
        baseYear: Int,
        entity: Entity?,
        inflationRate: Double,
    ): Money {
        if (phases.isNotEmpty() && overrides.isEmpty()) {
            val matchingPhase = phases.firstOrNull { it.isApplicableInYear(calendarYear, entity) } ?: return Money.ZERO
            val sYear = matchingPhase.effectiveStartYear(entity)
            val yearsActive = (calendarYear - sYear).coerceAtLeast(0)
            val raiseFactor = (1.0 + yearlyPayBumpRate).pow(yearsActive.toDouble())
            val yearsFromBase = (calendarYear - baseYear).coerceAtLeast(0)
            val infFactor = if (inflationAdjusted) (1.0 + inflationRate).pow(yearsFromBase.toDouble()) else 1.0
            val totalCents = (matchingPhase.amount.value * raiseFactor * infFactor).toLong()
            return Money(totalCents)
        }

        val baseStartYear = effectiveStartYear(entity)
        if (calendarYear < baseStartYear) return Money.ZERO

        if (overrides.isEmpty()) {
            val baseEndYear = effectiveEndYear(entity)
            if (calendarYear > baseEndYear) return Money.ZERO
        }

        var activeStartYear = baseStartYear
        var activeBaseAmount = initialAnnualAmount
        var activeBumpRate = yearlyPayBumpRate

        val sortedOverrides = overrides.sortedBy { it.effectiveStartYear(entity) }
        for (ov in sortedOverrides) {
            val ovStart = ov.effectiveStartYear(entity)
            if (ovStart <= calendarYear) {
                activeStartYear = ovStart
                ov.annualAmount?.let { activeBaseAmount = it }
                ov.yearlyPayBumpRate?.let { activeBumpRate = it }
            }
        }

        if (activeBaseAmount.value <= 0L) return Money.ZERO

        val yearsActive = (calendarYear - activeStartYear).coerceAtLeast(0)
        val raiseFactor = (1.0 + activeBumpRate).pow(yearsActive.toDouble())
        val yearsFromBase = (calendarYear - baseYear).coerceAtLeast(0)
        val infFactor = if (inflationAdjusted) (1.0 + inflationRate).pow(yearsFromBase.toDouble()) else 1.0
        val totalCents = (activeBaseAmount.value * raiseFactor * infFactor).toLong()
        return Money(totalCents)
    }

    // Compatibility methods
    fun isApplicableAtAge(age: Int): Boolean = age in startAge..endAge

    fun amountAtAge(
        age: Int,
        baseAge: Int,
        inflationRate: Double,
    ): Money {
        if (!isApplicableAtAge(age)) return Money.ZERO
        val yearsActive = (age - startAge).coerceAtLeast(0)
        val raiseFactor = (1.0 + yearlyPayBumpRate).pow(yearsActive.toDouble())
        val yearsFromBase = (age - baseAge).coerceAtLeast(0)
        val infFactor = if (inflationAdjusted) (1.0 + inflationRate).pow(yearsFromBase.toDouble()) else 1.0
        val totalCents = (initialAnnualAmount.value * raiseFactor * infFactor).toLong()
        return Money(totalCents)
    }
}
