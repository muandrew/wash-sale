package com.muandrew.stock.model

import com.muandrew.money.Money
import com.squareup.moshi.Json
import java.time.LocalDate

sealed interface Transaction {
    val date: LocalDate
    val referenceNumber: String?
    @Json(ignore = true)
    val ref: TransactionReference
        get() = TransactionReference(
            date = date,
            referenceNumber = referenceNumber,
        )

    data class SaleTransaction(
        override val date: LocalDate,
        val value: Money,
        val shares: Long,
        val lotId: LotReference,
        override val referenceNumber: String? = null,
    ) : Transaction

    data class ReleaseTransaction(
        override val date: LocalDate,
        val disbursed: LotValue,
        override val referenceNumber: String? = null,
    ) : Transaction
}