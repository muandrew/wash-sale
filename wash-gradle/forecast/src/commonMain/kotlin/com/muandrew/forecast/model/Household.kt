package com.muandrew.forecast.model

import kotlin.math.pow

data class Household(
    val id: String,
    val name: String,
    val isPrimary: Boolean = true,
    val baseYear: Int = 2026,
    val entities: List<Entity> = listOf(
        Entity("primary_person", "Primary Earner", birthYear = 1996, isPrimary = true, retirementAge = 60, lifeExpectancy = 90)
    ),
    val annualIncome: Money = Money.ofDollars(120_000),
    val incomeStreams: List<IncomeStream> = emptyList(),
    val assetPools: List<AssetPool> = emptyList(),
    val expenses: List<ExpenseItem> = emptyList(),
    val directExpenses: List<ExpenseItem> = emptyList(),
    val lifeEvents: List<LifeEvent> = emptyList()
) {
    fun primaryEntity(): Entity {
        return entities.firstOrNull { it.isPrimary }
            ?: entities.firstOrNull()
            ?: Entity("default", name, birthYear = baseYear - 30, isPrimary = true)
    }

    fun findEntity(id: String): Entity? {
        return entities.firstOrNull { it.id == id } ?: entities.firstOrNull { it.isPrimary } ?: entities.firstOrNull()
    }

    val currentAge: Int get() = primaryEntity().ageInYear(baseYear)
    val retirementAge: Int get() = primaryEntity().retirementAge
    val lifeExpectancy: Int get() = primaryEntity().lifeExpectancy

    fun allExpenses(): List<ExpenseItem> {
        val eventExpenses = lifeEvents.flatMap { it.generateExpenses(currentAge) }
        return expenses + directExpenses + eventExpenses
    }

    fun allAssetPools(): List<AssetPool> {
        val eventAssets = lifeEvents.flatMap { it.generateAssetPools() }
        return assetPools + eventAssets
    }

    fun totalInitialNetWorth(): Money {
        val sum = allAssetPools().sumOf { it.currentBalance.value }
        return Money(sum)
    }

    /**
     * Calculates total household income in a specific calendar year.
     */
    fun totalIncomeInYear(calendarYear: Int, inflationRate: Double): Money {
        if (incomeStreams.isNotEmpty()) {
            val totalCents = incomeStreams.sumOf { stream ->
                val entity = findEntity(stream.entityId)
                stream.amountInYear(calendarYear, baseYear, entity, inflationRate).value
            }
            return Money(totalCents)
        } else {
            val prim = primaryEntity()
            val age = prim.ageInYear(calendarYear)
            if (age < prim.retirementAge) {
                val years = (calendarYear - baseYear).coerceAtLeast(0)
                val inflationFactor = (1.0 + inflationRate).pow(years.toDouble())
                return Money((annualIncome.value * inflationFactor).toLong())
            }
            return Money.ZERO
        }
    }

    /**
     * Backward-compatible helper for age-based lookup.
     */
    fun totalIncomeAtAge(age: Int, inflationRate: Double): Money {
        val calendarYear = primaryEntity().yearAtAge(age)
        return totalIncomeInYear(calendarYear, inflationRate)
    }
}

data class FinancialPlan(
    val id: String = "default_plan",
    val name: String = "Master Financial Plan",
    val baseYear: Int = 2026,
    val households: List<Household> = emptyList(),
    val inflationRate: Double = 0.025
) {
    fun totalInitialNetWorth(): Money {
        val sum = households.sumOf { it.totalInitialNetWorth().value }
        return Money(sum)
    }
}
