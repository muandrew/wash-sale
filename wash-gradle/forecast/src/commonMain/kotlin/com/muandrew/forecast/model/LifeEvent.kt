package com.muandrew.forecast.model

sealed interface LifeEvent {
    val id: String
    val name: String

    fun generateExpenses(parentCurrentAge: Int): List<ExpenseItem>

    fun generateAssetPools(): List<AssetPool> = emptyList()
}

/**
 * Adding a Child template with 4 distinct lifecycle stages:
 * 1. Daycare / Early Childcare (ages 0-5)
 * 2. School-Age Living & Extracurriculars (ages 6-17)
 * 3. 529 College Savings contributions (ages 0-17)
 * 4. College / Higher Education Tuition (ages 18-21)
 */
data class ChildTemplate(
    override val id: String,
    val childName: String,
    val parentAgeAtBirth: Int,
    val annualDaycareCost: Money = Money.ofDollars(18_000),
    val annualSchoolAgeLivingCost: Money = Money.ofDollars(8_000),
    val annual529Contribution: Money = Money.ofDollars(6_000),
    val annualCollegeTuition: Money = Money.ofDollars(35_000),
) : LifeEvent {
    override val name: String = "Child: $childName"

    override fun generateExpenses(parentCurrentAge: Int): List<ExpenseItem> {
        val expenses = mutableListOf<ExpenseItem>()

        // Stage 1: Daycare / Early Childcare (Child ages 0 to 5)
        expenses.add(
            ExpenseItem(
                id = "${id}_daycare",
                name = "$childName - Daycare & Early Childcare (Ages 0-5)",
                category = ExpenseCategory.CHILDCARE_EARLY,
                annualAmount = annualDaycareCost,
                startAge = parentAgeAtBirth,
                endAge = parentAgeAtBirth + 5,
            ),
        )

        // Stage 2: School-Age Living & Healthcare (Child ages 6 to 17)
        expenses.add(
            ExpenseItem(
                id = "${id}_living",
                name = "$childName - School-Age Living & Healthcare (Ages 6-17)",
                category = ExpenseCategory.LIVING_ESSENTIALS,
                annualAmount = annualSchoolAgeLivingCost,
                startAge = parentAgeAtBirth + 6,
                endAge = parentAgeAtBirth + 17,
            ),
        )

        // Stage 3: 529 College Savings Contributions (Child ages 0 to 17)
        if (annual529Contribution.value > 0) {
            expenses.add(
                ExpenseItem(
                    id = "${id}_529",
                    name = "$childName - 529 College Savings Contribution",
                    category = ExpenseCategory.EDUCATION_TUITION,
                    annualAmount = annual529Contribution,
                    startAge = parentAgeAtBirth,
                    endAge = parentAgeAtBirth + 17,
                ),
            )
        }

        // Stage 4: Higher Education / College Tuition (Child ages 18 to 21)
        expenses.add(
            ExpenseItem(
                id = "${id}_college",
                name = "$childName - College Tuition & Living (Ages 18-21)",
                category = ExpenseCategory.EDUCATION_TUITION,
                annualAmount = annualCollegeTuition,
                startAge = parentAgeAtBirth + 18,
                endAge = parentAgeAtBirth + 21,
            ),
        )

        return expenses
    }
}

/**
 * Home Purchase Template:
 * Models down payment lump sum, monthly mortgage + property tax + maintenance, and real estate asset appreciation.
 */
data class HomePurchaseTemplate(
    override val id: String,
    val propertyName: String,
    val parentAgeAtPurchase: Int,
    val propertyValue: Money = Money.ofDollars(600_000),
    val downPaymentPercent: Double = 0.20,
    val annualMortgageAndTax: Money = Money.ofDollars(36_000),
    val loanTermYears: Int = 30,
) : LifeEvent {
    override val name: String = "Home Purchase: $propertyName"

    override fun generateExpenses(parentCurrentAge: Int): List<ExpenseItem> {
        val downPayment = Money((propertyValue.value * downPaymentPercent).toLong())
        return listOf(
            ExpenseItem(
                id = "${id}_downpayment",
                name = "$propertyName - Down Payment",
                category = ExpenseCategory.HOUSING_MORTGAGE,
                annualAmount = downPayment,
                startAge = parentAgeAtPurchase,
                endAge = parentAgeAtPurchase,
            ),
            ExpenseItem(
                id = "${id}_mortgage",
                name = "$propertyName - Mortgage & Property Tax",
                category = ExpenseCategory.HOUSING_MORTGAGE,
                annualAmount = annualMortgageAndTax,
                startAge = parentAgeAtPurchase + 1,
                endAge = parentAgeAtPurchase + loanTermYears,
            ),
        )
    }

    override fun generateAssetPools(): List<AssetPool> = listOf(
        AssetPool(
            id = "${id}_asset",
            name = propertyName,
            category = AssetCategory.REAL_ESTATE,
            currentBalance = propertyValue,
            expectedNominalReturn = 0.045,
            returnVolatility = 0.06,
        ),
    )
}

/**
 * Recurring Vacation / Discretionary Budget Template
 */
data class VacationBudgetTemplate(
    override val id: String,
    val budgetName: String,
    val annualBudget: Money = Money.ofDollars(8_000),
    val startAge: Int = 30,
    val endAge: Int = 85,
) : LifeEvent {
    override val name: String = "Vacation: $budgetName"

    override fun generateExpenses(parentCurrentAge: Int): List<ExpenseItem> = listOf(
        ExpenseItem(
            id = "${id}_vacation",
            name = budgetName,
            category = ExpenseCategory.DISCRETIONARY_VACATION,
            annualAmount = annualBudget,
            startAge = startAge,
            endAge = endAge,
        ),
    )
}
