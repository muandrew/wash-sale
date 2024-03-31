package com.muandrew.stock.world

import com.muandrew.money.Money
import com.muandrew.stock.model.*
import com.muandrew.stock.time.max
import java.time.LocalDate

class World {

    val releaseIds: MutableMap<LocalDate, Int> = mutableMapOf()
    val transactions: MutableList<Transaction> = mutableListOf()
    val lots: MutableList<Lot> = mutableListOf()
    val events: MutableList<TransactionReport> = mutableListOf()

    fun nextLotId(date: LocalDate): String {
        val idx = releaseIds[date] ?: 0
        releaseIds[date] = idx + 1
        return "$date.$idx"
    }

    fun acceptTransaction(transaction: Transaction) {
        when (transaction) {
            is Transaction.ReleaseTransaction -> {
                lots.add(
                    Lot.create(
                        runId = nextLotId(transaction.date.date),
                        date = transaction.date,
                        initial = LotValue(
                            shares = transaction.shares,
                            value = transaction.value,
                        ),
                        sourceTransaction = TransactionReference.DateReference(date = transaction.date)
                    )
                )
                events.add(
                    TransactionReport.ReceivedReport(
                        transaction.date,
                        transaction.shares,
                        transaction.value
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
                    val lotToSellFrom = lots.findFirstLotForId(transaction.lotId)
                        ?: throw IllegalStateException("couldn't find applicable lot(s) based on id: ${transaction.lotId}")
                    val lotShareValue = lotToSellFrom.current
                    val shares = minShares(saleRemaining, lotShareValue)
                    val (fromSale, fromSaleRem) = saleRemaining.splitOut(shares)
                    val (fromLot, fromLotRem) = lotShareValue.splitOut(shares)

                    val net = fromSale.value - fromLot.value
                    // TODO track transaction in lot
                    lotToSellFrom.current = fromLotRem
                    saleRemaining = fromSaleRem

                    if (net >= Money.ZERO) {
                        // profit!
                        val saleRecord = TransactionReport.SaleReport.SaleRecord(
                            soldLotId = lotToSellFrom.runId,
                            shares = fromLot.shares,
                            basis = fromLot.value,
                        )
                        allowedRecords.add(saleRecord)
                    } else {
                        // need to look for wash sale
                        var sharesToWash = LotValue(shares, net)
                        var trackBasisFromLot = fromLot
                        while (sharesToWash.shares > 0) {
                            // check 30 days before and 30 days after
                            val washTarget = lots.queryForLotToWashTo(lotToSellFrom, transaction.date.date) ?: break
                            val numberOfSharesToTransfer = minShares(sharesToWash, washTarget.current)
                            val (sharesToWashSplit, sharesToWashRem) = sharesToWash.splitOut(numberOfSharesToTransfer)
                            val (washTargetSplit, washTargetRem) = washTarget.current.splitOut(numberOfSharesToTransfer)
                            val (trackBasisFromLotSplit, trackBasisFromLotRem) = trackBasisFromLot.splitOut(
                                numberOfSharesToTransfer
                            )
                            trackBasisFromLot = trackBasisFromLotRem
                            // TODO track transaction
                            washTarget.current = washTargetRem

                            // using subtraction since the value is negative from loss.
                            val newLot = LotValue(
                                numberOfSharesToTransfer,
                                washTargetSplit.value - sharesToWashSplit.value
                            )
                            val washNumber = washTarget.numberOfWashes++
                            val washLot = Lot(
                                runId = "${washTarget.runId}.w:$washNumber",
                                date = lotToSellFrom.date,
                                // use the new time for calculating long/short term sale
                                overrideDateForSalesCalculation = max(lotToSellFrom.date, washTarget.date),
                                initial = newLot,
                                current = newLot,
                                transformed = TransformedFrom.WashSale(
                                    originalLot = LotReference.DateLotReference(lotToSellFrom.date),
                                    fromTransaction = TransactionReference.DateReference(transaction.date)
                                )
                            )
                            addNewWashSaleLot(washLot)

                            sharesToWash = sharesToWashRem

                            val washRecord = TransactionReport.SaleReport.WashRecord(
                                soldLotId = lotToSellFrom.runId,
                                transferredLotId = washTarget.runId,
                                resultingId = washLot.runId,
                                shares = assertEqual(washLot.current.shares, trackBasisFromLotSplit.shares),
                                basis = trackBasisFromLotSplit.value,
                                disallowedValue = sharesToWashSplit.value,
                            )
                            washedRecords.add(washRecord)
                        }
                        // check for remaining shares
                        if (sharesToWash.shares > 0) {
                            val saleRecord = TransactionReport.SaleReport.SaleRecord(
                                soldLotId = lotToSellFrom.runId,
                                shares = assertEqual(trackBasisFromLot.shares, sharesToWash.shares),
                                basis = trackBasisFromLot.value,
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
                        date = transaction.date,
                        shares = transaction.shares,
                        saleValue = transaction.value,
                        basisBeforeAdjustment = basisBeforeAdjustment,
                        disallowedValue = Money(washedRecords.sumOf { it.disallowedValue.value }),
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
            //TODO warning, need to use the whole datetime
            if (lotc.date.date <= lot.date.date) {
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
    assert(a == b)
    return a
}

fun List<Lot>.queryForLotToWashTo(
    sourceLot: Lot,
    saleDate: LocalDate
): Lot? {
    val before = saleDate.minusDays(30)
    val after = saleDate.plusDays(30)
    return firstOrNull {
        it != sourceLot &&
                it.date.date >= before &&
                it.date.date <= after &&
                it.current.shares > 0 &&
                !it.isReplacement
    }
}

operator fun LocalDate.compareTo(other: LocalDate): Int {
    return if (isBefore(other)) {
        -1
    } else if (isAfter(other)) {
        1
    } else {
        0
    }
}

fun List<Lot>.findFirstLotForId(id: LotReference): Lot? {
    return when (id) {
        is LotReference.DateLotReference -> {
            val time = id.date.time
            // picking very specific lot
            if (time != null) {
                firstOrNull {
                    it.current.shares > 0 && it.date == id.date
                }
                // picking fifo for the day
            } else {
                firstOrNull {
                    it.current.shares > 0 && it.date.date == id.date.date
                }
            }
        }
    }
}