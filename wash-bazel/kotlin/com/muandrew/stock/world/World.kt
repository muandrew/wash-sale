package com.muandrew.stock.world

import com.github.doyaaaaaken.kotlincsv.client.ICsvFileWriter
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.muandrew.money.Money
import com.muandrew.stock.model.*
import com.muandrew.stock.time.max
import java.time.LocalDate

class World(val ignoreWash: Boolean = false) {

    val releaseIds: MutableMap<LocalDate, Int> = mutableMapOf()
    val transactions: MutableList<Transaction> = mutableListOf()
    val lots: MutableList<Lot> = mutableListOf()
    val events: MutableList<TransactionReport> = mutableListOf()

    fun nextLotId(date: LocalDate): Pair<Int, String> {
        // start with 1 to match bank
        val idx = releaseIds[date] ?: 1
        releaseIds[date] = idx + 1
        return idx to "$date.$idx"
    }

    fun processTransaction(transaction: Transaction) {
        when (transaction) {
            is Transaction.ReleaseTransaction -> {
                val (lotno, runId) = nextLotId(transaction.date)
                lots.add(
                    Lot(
                        runId = runId,
                        lot = lotno,
                        date = transaction.date,
                        initial = transaction.disbursed,
                        wireCurrent = transaction.disbursed,
                        sourceLot = null,
                        sourceTransaction = TransactionReference(
                            date = transaction.date,
                            referenceNumber = transaction.referenceNumber
                        )
                    )
                )
                events.add(
                    TransactionReport.ReceivedReport(
                        transaction.ref,
                        transaction.disbursed.shares,
                        transaction.disbursed.value
                    )
                )
            }

            is Transaction.SaleTransaction -> {
                var saleRemaining = LotValue(
                    shares = transaction.shares,
                    value = transaction.value,
                )
                val allowedRecords = mutableListOf<TransactionReport.SaleReport.SaleRecord>()
                val washedRecords = mutableListOf<TransactionReport.SaleReport.WashRecord>()

                while (saleRemaining != LotValue.ZERO) {
                    val lotToSellFrom = lots.findAvailableLotToSellFrom(transaction.lotId)
                        ?: throw IllegalStateException("couldn't find applicable lot(s) based on id: ${transaction.lotId}")
                    val lotShareValue = lotToSellFrom.current
                    val shares = minShares(saleRemaining, lotShareValue)
                    val (fromSale, fromSaleRem) = saleRemaining.splitOut(shares)
                    val (fromLot, fromLotRem) = lotShareValue.splitOut(shares)

                    val net = fromSale.value - fromLot.value
                    lotToSellFrom.updateLotValue(transaction.ref, fromLotRem)
                    saleRemaining = fromSaleRem

                    if (net >= Money.ZERO || ignoreWash) {
                        // profit!
                        val saleRecord = TransactionReport.SaleReport.SaleRecord(
                            soldLotId = lotToSellFrom.runId,
                            soldLotDateForSalesCalculation = lotToSellFrom.dateForSales,
                            shares = assertEqual(fromLot.shares, fromSale.shares),
                            basis = fromLot.value,
                            gross = fromSale.value,
                        )
                        allowedRecords.add(saleRecord)
                    } else {
                        // need to look for wash sale
                        var basisFromLot = fromLot
                        var grossFromSale = fromSale
                        while (grossFromSale.shares > 0) {
                            // check 30 days before and 30 days after
                            val washTarget = lots.findLotToWashTo(lotToSellFrom, transaction.date) ?: break
                            val numberOfSharesToTransfer = minShares(grossFromSale, washTarget.current)
                            val (washTargetSplit, washTargetRem) = washTarget.current.splitOut(numberOfSharesToTransfer)
                            val (basisFromLotSplit, basisFromLotRem) = basisFromLot.splitOut(numberOfSharesToTransfer)
                            val (grossFromSaleSplit, grossFromSaleRem) = grossFromSale.splitOut(numberOfSharesToTransfer)
                            basisFromLot = basisFromLotRem
                            grossFromSale = grossFromSaleRem
                            washTarget.updateLotValue(transaction.ref, washTargetRem)

                            val newNet = grossFromSaleSplit.value - basisFromLotSplit.value
                            // using subtraction since the value is negative from loss.
                            val newLot = LotValue(
                                numberOfSharesToTransfer,
                                washTargetSplit.value - newNet
                            )
                            val washNumber = washTarget.nextWashNumber++
                            val washLot = Lot(
                                runId = "${washTarget.runId}.w:$washNumber",
                                lot = washTarget.lot,
                                date = washTarget.date,
                                // use the new time for calculating long/short term sale
                                overrideDateForSalesCalculation = max(lotToSellFrom.date, washTarget.date),
                                initial = newLot,
                                sourceLot = lotToSellFrom.ref,
                                sourceTransaction = TransactionReference(
                                    date = transaction.date,
                                    referenceNumber = transaction.referenceNumber,
                                )
                            )
                            addNewWashSaleLot(washLot)

                            val washRecord = TransactionReport.SaleReport.WashRecord(
                                soldLotId = lotToSellFrom.runId,
                                soldLotDateForSalesCalculation = lotToSellFrom.dateForSales,
                                transferredLotId = washTarget.runId,
                                resultingId = washLot.runId,
                                shares = assertEqual(washLot.current.shares, basisFromLotSplit.shares),
                                basis = basisFromLotSplit.value,
                                gross = grossFromSaleSplit.value,
                            )
                            washedRecords.add(washRecord)
                        }
                        // check for remaining shares
                        if (grossFromSale.shares > 0) {
                            assertEqual(basisFromLot.shares, grossFromSale.shares)
                            val saleRecord = TransactionReport.SaleReport.SaleRecord(
                                soldLotId = lotToSellFrom.runId,
                                soldLotDateForSalesCalculation = lotToSellFrom.dateForSales,
                                shares = assertEqual(basisFromLot.shares, grossFromSale.shares),
                                basis = basisFromLot.value,
                                gross = grossFromSale.value,
                            )
                            allowedRecords.add(saleRecord)
                        }
                    }
                }

                val basisBeforeAdjustment = Money(
                    allowedRecords.sumOf { it.basis.value } + washedRecords.sumOf { it.basis.value }
                )

                events.add(
                    TransactionReport.SaleReport(
                        transaction.ref,
                        shares = transaction.shares,
                        saleValue = transaction.value,
                        basisBeforeAdjustment = basisBeforeAdjustment,
                        disallowedValue = Money(washedRecords.sumOf { it.net.value }),
                        disallowedTransfer = washedRecords,
                        allowedTransfer = allowedRecords,
                    )
                )
            }
        }
        transactions.add(transaction)
    }

    private fun addNewWashSaleLot(lot: Lot) {
        var addIndex = 0
        var i = 0
        for (lotc in lots) {
            i++
            if (lotc.date <= lot.date) {
                addIndex = i
            } else {
                break
            }
        }
        lots.add(addIndex, lot)
    }

    override fun toString(): String {
        return "{lots: $lots}"
    }
}

fun <T : Any> assertEqual(a: T, b: T): T {
    if (a != b) {
        throw IllegalStateException("should be equal $a, $b")
    }
    return a
}

fun List<Lot>.findLotToWashTo(
    sourceLot: Lot,
    saleDate: LocalDate
): Lot? {
    val before = saleDate.minusDays(30)
    val after = saleDate.plusDays(30)
    return firstOrNull {
        val res = it != sourceLot &&
                it.date >= before &&
                it.date <= after &&
                it.date.year == saleDate.year &&
                it.current.shares > 0 &&
                !it.isReplacement
        res
    }
}

fun List<Lot>.findAvailableLotToSellFrom(id: LotReference): Lot? {
    // picking fifo for the day
    return firstOrNull {
        val chk1 = it.current.shares > 0 && it.date == id.date
        chk1 && if (id.lotId != null) {
            it.lot == id.lotId
        } else true
    }
}

fun List<Lot>.filterByReference(id: LotReference): List<Lot> {
    // picking fifo for the day
    return filter {
        val chk1 = it.date == id.date
        chk1 && if (id.lotId != null) {
            it.lot == id.lotId
        } else true
    }
}

data class BPD(
    var shares: Long = 0,
    var basis: Money = Money.ZERO,
    var gross: Money = Money.ZERO,
    var disallowed: Money = Money.ZERO,
)

data class SaleReport1099(
    val ref: TransactionReference,
    var shortTerm: BPD = BPD(),
    var longTerm: BPD = BPD(),
)

fun List<TransactionReport>.generate1099Report(filePath: String) {
    val interestingReports = filterIsInstance<TransactionReport.SaleReport>()
        .groupingBy { it.ref }
        .aggregate { key, accumulator: SaleReport1099?, element, first ->
            val report = accumulator ?: SaleReport1099(element.ref)

            element.disallowedTransfer.forEach {
                val bpd = if (it.soldLotDateForSalesCalculation.plusYears(1) < element.ref.date) {
                    report.longTerm
                } else {
                    report.shortTerm
                }
                bpd.shares += it.shares
                bpd.basis += it.basis
                bpd.gross += it.gross
                bpd.disallowed += it.net
            }
            element.allowedTransfer.forEach {
                val bpd = if (it.soldLotDateForSalesCalculation.plusYears(1) < element.ref.date) {
                    report.longTerm
                } else {
                    report.shortTerm
                }
                bpd.shares += it.shares
                bpd.basis += it.basis
                bpd.gross += it.gross
            }
            report
        }
    csvWriter {
        this.delimiter = '\t'
    }.open(targetFileName = filePath, append = false) {
        this.writeRow(
            "long/short",
            "date",
            "reference",
            "shares",
            "gross",
            "basis",
            "gross-basis",
            "disallowed as positive",
            "final"
        )
        interestingReports.values.sortedBy { it.ref }.forEach {
            maybeWriteRow("long", it.ref, it.longTerm)
            maybeWriteRow("short", it.ref, it.shortTerm)
        }
    }
}

fun ICsvFileWriter.maybeWriteRow(longShort: String, ref: TransactionReference, data: BPD) {
    if (data.shares == 0L) {
        return
    }
    val net = data.gross - data.basis
    writeRow(
        longShort,
        ref.date,
        ref.referenceNumber,
        data.shares,
        data.gross,
        data.basis,
        net,
        data.disallowed * -1,
        net - data.disallowed,
    )
}
