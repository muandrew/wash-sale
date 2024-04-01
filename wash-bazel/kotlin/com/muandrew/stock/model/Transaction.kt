package com.muandrew.stock.model

import com.muandrew.money.Money
import com.muandrew.stock.time.DateTime
import java.time.LocalDate

sealed interface Transaction {
    val date: DateTime

    data class SaleTransaction(
        override val date: DateTime,
        val value: Money,
        val shares: Long,
        val lotId: LotReference,
    ) : Transaction

    data class ReleaseTransaction(
        override val date: DateTime,
        val disbursed: LotValue,
    ) : Transaction

    companion object {
        fun createRelease(date: LocalDate, shares: Long, value: Money): ReleaseTransaction {
            val dateTime = DateTime(date = date)
            return  ReleaseTransaction(
                date = dateTime,
                disbursed = LotValue(shares, value),
            )
        }

        fun createSale(
            date: LocalDate,
            shares: Long,
            value: Money,
            lotDate: LocalDate,
        ): SaleTransaction {
            val dateTime = DateTime(date = date)
            return SaleTransaction(
                date = dateTime,
                value = value,
                shares = shares,
                lotId = LotReference.DateLotReference(date = DateTime(date = lotDate))
            )
        }
    }
}