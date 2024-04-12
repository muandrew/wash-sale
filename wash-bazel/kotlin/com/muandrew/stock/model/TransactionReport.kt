package com.muandrew.stock.model

import com.muandrew.money.Money
import com.squareup.moshi.Json
import java.time.LocalDate

sealed interface TransactionReport {

    val ref: TransactionReference

    data class ReceivedReport(
        override val ref: TransactionReference,
        val shares: Long,
        val costBasis: Money,
    ) : TransactionReport

    data class SaleReport(
        override val ref: TransactionReference,
        val shares: Long,
        val saleValue: Money,
        val basisBeforeAdjustment: Money,
        val disallowedValue: Money,
        val disallowedTransfer: List<WashRecord>,
        val allowedTransfer: List<SaleRecord>,
    ) : TransactionReport {

        data class WashRecord(
            val soldLotId: String,
            val soldLotDateForSalesCalculation: LocalDate,
            val soldLotDate: LocalDate,
            val transferredLotId: String,
            val resultingId: String,
            val shares: Long,
            val basis: Money,
            val gross: Money,
        ){
            @Json(ignore = true)
            val net get() = gross - basis
        }

        data class SaleRecord(
            val soldLotId: String,
            val soldLotDateForSalesCalculation: LocalDate,
            val soldLotDate: LocalDate,
            val shares: Long,
            val basis: Money,
            val gross: Money,
        ) {
            @Json(ignore = true)
            val net get() = gross - basis
        }
    }
}