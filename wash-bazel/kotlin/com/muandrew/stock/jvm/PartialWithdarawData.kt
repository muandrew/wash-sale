package com.muandrew.stock.jvm

import com.muandrew.money.Money
import com.muandrew.stock.jvm.StatementParser.toDate
import com.muandrew.stock.jvm.StatementParser.toLong
import com.muandrew.stock.jvm.StatementParser.toMoney
import java.time.LocalDate

data class PartialWithdarawData(
    val referenceNumber: String,
    val settlementDate: LocalDate,
    // val marketPricePerUnit: Money,
    val sharesSold: Long,
    val netProceeds: Money,
)

fun Map<String, String>.toPartialWithdarawData(): PartialWithdarawData {
    return PartialWithdarawData(
        referenceNumber = this["Reference Number"]!!,
        settlementDate = this.toDate("Settlement Date"),
        sharesSold = this.toLong("Shares Sold"),
        netProceeds = this.toMoney("Net Proceeds")
    )
}