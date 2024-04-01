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
    ) : Transaction

    data class ReleaseTransaction(
        override val date: LocalDate,
        val disbursed: LotValue,
    ) : Transaction
}