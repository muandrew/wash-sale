package com.muandrew.stock.jvm

import com.muandrew.money.Money
import com.muandrew.stock.jvm.StatementParser.asDate
import com.muandrew.stock.jvm.StatementParser.asLong
import com.muandrew.stock.jvm.StatementParser.asMoney
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
        settlementDate = this.asDate("Settlement Date"),
        sharesSold = this.asLong("Shares Sold"),
        netProceeds = this.asMoney("Net Proceeds")
    )
}