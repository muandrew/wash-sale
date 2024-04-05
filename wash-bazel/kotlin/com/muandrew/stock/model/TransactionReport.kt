package com.muandrew.stock.model

import com.muandrew.money.Money
import com.squareup.moshi.Json
import java.time.LocalDate

sealed interface TransactionReport {

    val ref: TransactionReference

    fun print()

    data class ReceivedReport(
        override val ref: TransactionReference,
        val shares: Long,
        val costBasis: Money,
    ) : TransactionReport {
        override fun print() {
            val valuePerShareWithRem = costBasis / shares
            println("${ref.date}: rcv $shares share(s) totalling ${costBasis}. [${valuePerShareWithRem.res} per share]")
        }
    }

    data class SaleReport(
        override val ref: TransactionReference,
        val shares: Long,
        val saleValue: Money,
        val basisBeforeAdjustment: Money,
        val disallowedValue: Money,
        val disallowedTransfer: List<WashRecord>,
        val allowedTransfer: List<SaleRecord>,
    ) : TransactionReport {
        override fun print() {
            val net = saleValue - basisBeforeAdjustment
            println("${ref.date}: sld $shares share(s) for $saleValue against cost basis of $basisBeforeAdjustment. [net: $net]")
        }

        data class WashRecord(
            val soldLotId: String,
            val soldLotDateForSalesCalculation: LocalDate,
            val transferredLotId: String,
            val resultingId: String,
            val shares: Long,
            val basis: Money,
            val gross: Money,
            val disallowedValue: Money,
        ){
            @Json(ignore = true)
            val net get() = gross - basis
        }

        data class SaleRecord(
            val soldLotId: String,
            val soldLotDateForSalesCalculation: LocalDate,
            val shares: Long,
            val basis: Money,
            val gross: Money,
        ) {
            @Json(ignore = true)
            val net get() = gross - basis
        }
    }
}