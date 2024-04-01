package com.muandrew.stock.model

import com.muandrew.money.Money
import java.time.LocalDate

sealed interface Transaction {
    val date: LocalDate

    data class SaleTransaction(
        override val date: LocalDate,
        val value: Money,
        val shares: Long,
        val lotId: LotReference,
        val referenceNumber: String? = null
    ) : Transaction

    data class ReleaseTransaction(
        override val date: LocalDate,
        val disbursed: LotValue,
        val referenceNumber: String? = null
    ) : Transaction
}