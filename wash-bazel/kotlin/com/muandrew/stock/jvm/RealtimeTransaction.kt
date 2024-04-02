package com.muandrew.stock.jvm

import java.time.LocalDate

data class RealtimeTransaction(
    val costBasis: CostBasis,
    val listItem: ListItem,
) {
    data class ListItem(
        val referenceNumber: String,
    )
    data class CostBasis(
        val shortTerm: TermedBasis?,
        val longTerm: TermedBasis?,
//        val totalGainOrLossValue: Money,
    ) {
        data class TermedBasis(
            val rows: List<Lot>,
            val totalQuantity: String
        ) {
            data class Lot(
                val purchaseDate: LocalDate,
                val type: String,
                val lot: Int,
                val marketValuePerShare: Money,
                val quantity: String,
                val gainOrLossValue: Money
            )
        }
    }
    data class Money(
        val amount: String,
        val currency: String,
    )
}
