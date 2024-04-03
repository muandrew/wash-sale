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
    var current: LotValue,
    val sourceTransaction: TransactionReference,
    val overrideDateForSalesCalculation: LocalDate? = null,
    @Json(name = "isReplacement")
    val wireIsReplacement: Boolean? = null,
) {
    val isReplacement: Boolean get() = wireIsReplacement ?: false
    @Transient
    var nextWashNumber = 1
}

sealed interface TransformedFrom {
    data class WashSale(
        val originalLot: LotReference,
        val fromTransaction: TransactionReference,
    ) : TransformedFrom
}