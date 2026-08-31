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
    val lifeEvents: List<LifeEvent> = emptyList(),
    val priorityRules: List<PriorityRule> = emptyList()
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
     * Generates default priority rules based on active expenses and asset pools.
     */
    fun defaultPriorityRules(): List<PriorityRule> {
        val rules = mutableListOf<PriorityRule>()
        var rank = 1

        // 1. Debt Service
        allExpenses().filter { it.expenseType == ExpenseType.COMPOUNDING_DEBT }.forEach { debt ->
            rules.add(
                PriorityRule(
                    id = "rule_${debt.id}",
                    name = debt.name,
                    targetType = PriorityTargetType.EXPENSE_PAYOUT,
                    targetId = debt.id,
                    entityId = debt.entityId,
                    itemType = PriorityItemType.DEBT_SERVICE,
                    priorityRank = rank++
                )
            )
        }

        // 2. Essential Living & Childcare
        allExpenses().filter {
            it.expenseType != ExpenseType.COMPOUNDING_DEBT &&
            (it.category == ExpenseCategory.LIVING_ESSENTIALS || it.category == ExpenseCategory.CHILDCARE_EARLY || it.category == ExpenseCategory.HEALTHCARE || it.category == ExpenseCategory.HOUSING_MORTGAGE)
        }.forEach { exp ->
            rules.add(
                PriorityRule(
                    id = "rule_${exp.id}",
                    name = exp.name,
                    targetType = PriorityTargetType.EXPENSE_PAYOUT,
                    targetId = exp.id,
                    entityId = exp.entityId,
                    itemType = PriorityItemType.EXPENSE_ESSENTIAL,
                    priorityRank = rank++
                )
            )
        }

        // 3. Emergency Cash Reserve Pool
        allAssetPools().filter { it.category == AssetCategory.CASH_EMERGENCY }.forEach { pool ->
            rules.add(
                PriorityRule(
                    id = "rule_${pool.id}",
                    name = pool.name,
                    targetType = PriorityTargetType.POOL_CONTRIBUTION,
                    targetId = pool.id,
                    entityId = pool.entityId,
                    itemType = PriorityItemType.CASH_RESERVE,
                    priorityRank = rank++
                )
            )
        }

        // 4. Tax-Advantaged Retirement Pools
        allAssetPools().filter { it.category == AssetCategory.PRE_TAX_401K || it.category == AssetCategory.ROTH_IRA }.forEach { pool ->
            rules.add(
                PriorityRule(
                    id = "rule_${pool.id}",
                    name = pool.name,
                    targetType = PriorityTargetType.POOL_CONTRIBUTION,
                    targetId = pool.id,
                    entityId = pool.entityId,
                    itemType = PriorityItemType.TAX_ADVANTAGED_RETIREMENT,
                    priorityRank = rank++
                )
            )
        }

        // 5. 529 / Dependent Education Pools and Tuition Expenses
        allAssetPools().filter { it.name.contains("529", ignoreCase = true) || it.name.contains("Education", ignoreCase = true) }.forEach { pool ->
            if (rules.none { it.targetId == pool.id }) {
                rules.add(
                    PriorityRule(
                        id = "rule_${pool.id}",
                        name = pool.name,
                        targetType = PriorityTargetType.POOL_CONTRIBUTION,
                        targetId = pool.id,
                        entityId = pool.entityId,
                        itemType = PriorityItemType.DEPENDENT_EDUCATION_529,
                        priorityRank = rank++
                    )
                )
            }
        }
        allExpenses().filter { it.category == ExpenseCategory.EDUCATION_TUITION }.forEach { exp ->
            if (rules.none { it.targetId == exp.id }) {
                rules.add(
                    PriorityRule(
                        id = "rule_${exp.id}",
                        name = exp.name,
                        targetType = PriorityTargetType.EXPENSE_PAYOUT,
                        targetId = exp.id,
                        entityId = exp.entityId,
                        itemType = PriorityItemType.DEPENDENT_EDUCATION_529,
                        priorityRank = rank++
                    )
                )
            }
        }

        // 6. Discretionary & Milestone Expenses
        allExpenses().filter {
            it.category == ExpenseCategory.DISCRETIONARY_VACATION || it.category == ExpenseCategory.MILESTONE_OTHER
        }.forEach { exp ->
            if (rules.none { it.targetId == exp.id }) {
                rules.add(
                    PriorityRule(
                        id = "rule_${exp.id}",
                        name = exp.name,
                        targetType = PriorityTargetType.EXPENSE_PAYOUT,
                        targetId = exp.id,
                        entityId = exp.entityId,
                        itemType = PriorityItemType.EXPENSE_DISCRETIONARY,
                        priorityRank = rank++
                    )
                )
            }
        }

        // 7. Remaining Asset Pools
        allAssetPools().forEach { pool ->
            if (rules.none { it.targetId == pool.id }) {
                rules.add(
                    PriorityRule(
                        id = "rule_${pool.id}",
                        name = pool.name,
                        targetType = PriorityTargetType.POOL_CONTRIBUTION,
                        targetId = pool.id,
                        entityId = pool.entityId,
                        itemType = if (pool.category == AssetCategory.TAXABLE_BROKERAGE) PriorityItemType.TAXABLE_BROKERAGE_SURPLUS else PriorityItemType.CUSTOM_POOL,
                        priorityRank = rank++
                    )
                )
            }
        }

        // Remaining expenses not yet caught
        allExpenses().forEach { exp ->
            if (rules.none { it.targetId == exp.id }) {
                rules.add(
                    PriorityRule(
                        id = "rule_${exp.id}",
                        name = exp.name,
                        targetType = PriorityTargetType.EXPENSE_PAYOUT,
                        targetId = exp.id,
                        entityId = exp.entityId,
                        itemType = PriorityItemType.CUSTOM_EXPENSE,
                        priorityRank = rank++
                    )
                )
            }
        }

        return rules
    }

    fun activePriorityRules(): List<PriorityRule> {
        val existing = priorityRules.ifEmpty { defaultPriorityRules() }
        // Ensure any newly added pools or expenses are appended
        val defaults = defaultPriorityRules()
        val missing = defaults.filter { d -> existing.none { it.targetId == d.targetId } }
        return (existing + missing).sortedBy { it.priorityRank }
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
