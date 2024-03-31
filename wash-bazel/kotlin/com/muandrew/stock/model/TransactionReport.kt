package com.muandrew.stock.model

import com.muandrew.money.Money
import com.muandrew.stock.time.DateTime

sealed interface TransactionReport {

    fun print()

    data class ReceivedReport(
        val date: DateTime,
        val shares: Long,
        val costBasis: Money,
    ) : TransactionReport {
        override fun print() {
            val valuePerShareWithRem = costBasis / shares
            println("$date: rcv $shares share(s) totalling ${costBasis}. [${valuePerShareWithRem.res} per share]")
        }
    }

    data class SaleReport(
        val date: DateTime,
        val shares: Long,
        val saleValue: Money,
        val basisBeforeAdjustment: Money,
        val disallowedValue: Money,
        val disallowedTransfer: List<WashRecord>,
        val allowedTransfer: List<SaleRecord>,
    ) : TransactionReport {
        override fun print() {
            val net = saleValue - basisBeforeAdjustment
            println("$date: sld $shares share(s) for $saleValue against cost basis of $basisBeforeAdjustment. [net: $net]")
        }

        data class WashRecord(
            val soldLotId: String,
            val transferredLotId: String,
            val resultingId: String,
            val shares: Long,
            val basis: Money,
            val disallowedValue: Money,
        )

        data class SaleRecord(
            val soldLotId: String,
            val shares: Long,
            val basis: Money,
        )
    }

    data class MessageReport(val messages: List<String>): TransactionReport {
        override fun print() {
            println(messages.joinToString("\n"))
        }
    }
}