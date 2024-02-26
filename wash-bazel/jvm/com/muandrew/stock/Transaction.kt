package com.muandrew.stock

import com.muandrew.money.Money
import com.muandrew.stock.time.DateTime
import java.time.LocalDate

sealed interface Transaction {
    val id: TransactionId
    val date: DateTime

    data class SaleTransaction(
        override val id: TransactionId,
        override val date: DateTime,
        val value: Money,
        val shares: Long,
        val lotId: LotIdentifier,
    ) : Transaction

    data class ReleaseTransaction(
        override val id: TransactionId,
        override val date: DateTime,
        val value: Money,
        val shares: Long
    ) : Transaction

    companion object {
        fun createRelease(date: LocalDate, shares: Long, value: Money): ReleaseTransaction{
            val dateTime = DateTime(date = date)
            return  ReleaseTransaction(
                id = TransactionId.DateId(date = dateTime),
                date = dateTime,
                value = value,
                shares = shares,
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
                id = TransactionId.DateId(date = dateTime),
                date = dateTime,
                value = value,
                shares = shares,
                lotId = LotIdentifier.DateLotIdentifier(date = DateTime(date = lotDate))
            )
        }
    }
}