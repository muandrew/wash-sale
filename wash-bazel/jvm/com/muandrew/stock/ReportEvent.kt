package com.muandrew.stock

import com.muandrew.money.Money
import com.muandrew.stock.time.DateTime

sealed interface ReportEvent {

    fun print()

    data class ReceivedEvent(
        val date: DateTime,
        val shares: Long,
        val costBasis: Money,
    ) : ReportEvent {
        override fun print() {
            val valuePerShareWithRem = costBasis / shares
            println("$date: rcv $shares share(s) totalling ${costBasis}. [${valuePerShareWithRem.res} per share]")
        }
    }

    data class SaleEvent(
        val date: DateTime,
        val shares: Long,
        val saleValue: Money,
        val costBasis: Money,
    ) : ReportEvent {
        override fun print() {
            val net = saleValue - costBasis
            println("$date: sld $shares share(s) for $saleValue against cost basis of $costBasis. [net: $net]")
        }
    }

    data class WashSaleEvent(
        val date: DateTime,
        val allowedShares: Long,
        val allowedValue: Money,
        val disallowedShares: Long,
        val disallowedValue: Money,
    ) : ReportEvent {
        override fun print() {
            println(
                """
                detected wash sale:
                    allowed loss: $allowedShares, $allowedValue
                    disallowed loss: $disallowedShares, $disallowedValue
                """.trimIndent()
            )
        }

    }
}