package com.muandrew.stock.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDate

/**
 * @param overrideDateForSalesCalculation sometimes a different date is used, in case of wash sale
 */
@JsonClass(generateAdapter = true)
data class Lot(
    val runId: String,
    val lot: Int,
    val date: LocalDate,
    val initial: LotValue,
    val sourceTransaction: TransactionReference,
    val sourceLot: LotReference?,
    val overrideDateForSalesCalculation: LocalDate? = null,
    @Json(name = "current") var wireCurrent: LotValue = initial,
    @Json(name = "transactions") val wireTransactions: MutableList<LotChange> = mutableListOf(),
) {
    val isReplacement: Boolean get() = sourceLot != null
    val current: LotValue get() = wireCurrent
    val ref: LotReference get() = LotReference(date, lot)

    fun updateLotValue(transactionReference: TransactionReference, newValue: LotValue) {
        wireTransactions.add(LotChange(transactionReference, newValue - wireCurrent))
        wireCurrent = newValue
    }

    @Transient
    var nextWashNumber = 1
}

data class LotChange(
    val transactionReference: TransactionReference,
    val change: LotValue,
)
