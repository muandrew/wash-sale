package com.muandrew.stock.world

import com.muandrew.money.Money
import com.muandrew.stock.model.*
import com.muandrew.stock.time.DateTime
import java.time.LocalDate

class World {

    val transactions: MutableList<Transaction> = mutableListOf()
    val lots: MutableList<Lot> = mutableListOf()
    val events: MutableList<ReportEvent> = mutableListOf()

    fun acceptTransaction(transaction: Transaction) {
        when (transaction) {
            is Transaction.ReleaseTransaction -> {
                lots.add(
                    Lot.create(
                        date = transaction.date,
                        initial = ShareValue(
                            shares = transaction.shares,
                            value = transaction.value,
                        ),
                        sourceTransaction = TransactionReference.DateReference(date = transaction.date)
                    )
                )
                events.add(
                    ReportEvent.ReceivedEvent(
                        transaction.date,
                        transaction.shares,
                        transaction.value
                    )
                )
            }

            is Transaction.SaleTransaction -> {
                val saleSourceLots = lots.findLotsForId(transaction.lotId)
                if (saleSourceLots.isEmpty()) {
                    throw IllegalStateException("couldn't find applicable lot(s) based on id: ${transaction.lotId}")
                }

                val saleRes = applySharesAmongCandidates(
                    ShareValue(transaction.shares, transaction.value),
                    saleSourceLots,
                    Lot::current,
                    updateCandidate = {
                        val res = transactForBasis(TransactionReference.DateReference(date = transaction.date), it.shares)
                        ShareValue(it.shares, res)
                    },
                )
                assert(saleRes.targetRemaining.shares == 0L)
                assert(saleRes.targetRemaining.value == Money.ZERO)

                val costBasis = saleRes.accumulatedChanges.value
                val net = transaction.value - costBasis
                events.add(
                    ReportEvent.SaleEvent(
                        transaction.date,
                        transaction.shares,
                        transaction.value,
                        costBasis
                    )
                )
                // check for wash sale
                if (net < Money.ZERO) {
                    // check 30 days before and 30 days after
                    val washCandidates = lots.queryForWashSale(transaction.date.date)
                    val washRes = applySharesAmongCandidates(
                        ShareValue(transaction.shares, net),
                        washCandidates,
                        Lot::current,
                        updateCandidate = {
                            // shares split will be transferred to replacement lot
                            val res = current.splitOut(it.shares)
                            // TODO create replacement lot

                            // using subtraction since the value is negative from loss.
                            val newLot = ShareValue(it.shares, res.split.value - it.value)
                            addWashSale(
                                Lot(
                                    date = transaction.date, // use the new time for calculating long/short term sale
                                    initial = newLot,
                                    current = newLot,
                                    transformed = TransformedFrom.WashSale(
                                        originalLot = LotReference.DateLotReference(this.date),
                                        fromTransaction = TransactionReference.DateReference(transaction.date)
                                    )
                                ),
                                this.date,
                            )

                            current = res.remainder

                            // return wash disallowed
                            it
                        },
                    )
                    val washAllowed = washRes.targetRemaining
                    val accumulatedWashDisallowed = washRes.accumulatedChanges
                    events.add(
                        ReportEvent.WashSaleEvent(
                            transaction.date,
                            washAllowed.shares,
                            washAllowed.value,
                            accumulatedWashDisallowed.shares,
                            accumulatedWashDisallowed.value,
                        )
                    )
                }
            }
        }
        transactions.add(transaction)
    }

    fun addWashSale(lot: Lot, date: DateTime) {
        var addIndex = 0
        var i = 0
        for (lotc in lots) {
            i++
            //TODO warning, need to use the whole datetime
            if (lotc.date.date <= date.date) {
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

fun List<Lot>.queryForWashSale(date: LocalDate): List<Lot> {
    val before = date.minusDays(30)
    val after = date.plusDays(30)
    return filter { it.date.date >= before && it.date.date <= after && it.current.shares > 0 && !it.isReplacement }
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

fun List<Lot>.findLotsForId(id: LotReference): List<Lot> {
    return when (id) {
        is LotReference.DateLotReference -> {
            val time = id.date.time
            // picking very specific lot
            if (time != null) {
                filter {
                    it.current.shares > 0 && it.date == id.date
                }
                // picking fifo for the day
            } else {
                filter {
                    it.current.shares > 0 && it.date.date == id.date.date
                }
            }
        }
    }
}
