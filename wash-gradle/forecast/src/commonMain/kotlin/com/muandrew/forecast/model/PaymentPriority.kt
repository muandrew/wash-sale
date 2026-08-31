package com.muandrew.forecast.model

enum class PriorityItemType(val displayName: String, val hexColor: Long) {
    EXPENSE_ESSENTIAL("Living Essentials & Childcare", 0xFF64B5F6),
    DEBT_SERVICE("High-Interest Compounding Debt", 0xFFEF5350),
    CASH_RESERVE("Emergency Cash Reserve", 0xFFFFCA28),
    TAX_ADVANTAGED_RETIREMENT("401(k) / Roth IRA Retirement", 0xFF66BB6A),
    DEPENDENT_EDUCATION_529("529 Dependent Education", 0xFFFFA726),
    EXPENSE_DISCRETIONARY("Discretionary / Vacation", 0xFF42A5F5),
    TAXABLE_BROKERAGE_SURPLUS("Taxable Brokerage Surplus", 0xFFAB47BC),
    CUSTOM_POOL("Asset Pool Investment", 0xFF26A69A),
    CUSTOM_EXPENSE("Expense Payout", 0xFFEC407A)
}

enum class PriorityTargetType {
    EXPENSE_PAYOUT,
    POOL_CONTRIBUTION,
    SURPLUS_INVESTMENT
}

enum class FundingStatus(val displayName: String, val isProblem: Boolean) {
    FULLY_FUNDED("Fully Funded", false),
    PARTIALLY_FUNDED("Partially Funded", true),
    UNFUNDED("Unfunded / Missed", true),
    DEFICIT("Cashflow Deficit", true)
}

data class PriorityRule(
    val id: String,
    val name: String,
    val targetType: PriorityTargetType,
    val targetId: String,
    val entityId: String = "",
    val itemType: PriorityItemType,
    val priorityRank: Int,
    val enabled: Boolean = true
)

data class YearlyItemFunding(
    val id: String,
    val name: String,
    val entityId: String,
    val targetType: PriorityTargetType,
    val itemType: PriorityItemType,
    val targetAmount: Money,
    val actualAmount: Money,
    val shortfall: Money,
    val status: FundingStatus
)
