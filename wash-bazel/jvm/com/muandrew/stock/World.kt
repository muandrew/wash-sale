package com.muandrew.stock

import com.muandrew.money.Money
import com.muandrew.stock.lot.ShareValue
import com.muandrew.stock.lot.applySharesAmongCandidates
import com.muandrew.stock.lot.splitOut
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
                        id = LotIdentifier.DateLotIdentifier(transaction.date),
                        date = transaction.date,
                        initial = ShareValue(
                            shares = transaction.shares,
                            value = transaction.value,
                        ),
                        sourceTransaction = transaction.id
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
                        val res = transactForBasis(transaction.id, it.shares)
                        ShareValue(it.shares, res)
                    },
                )
                assert(saleRes.targetRemaining.shares == 0L)
                assert(saleRes.targetRemaining.value == Money.ZERO)

                val costBasis = saleRes.accumulatedChanges.value
                val net = transaction.value - costBasis
                events.add(ReportEvent.SaleEvent(
                    transaction.date,
                    transaction.shares,
                    transaction.value,
                    costBasis
                ))
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

                            current = res.remainder

                            // return wash disallowed
                            it
                        },
                    )
                    val washAllowed = washRes.targetRemaining
                    val accumulatedWashDisallowed = washRes.accumulatedChanges
                    events.add(ReportEvent.WashSaleEvent(
                        transaction.date,
                        washAllowed.shares,
                        washAllowed.value,
                        accumulatedWashDisallowed.shares,
                        accumulatedWashDisallowed.value,
                    ))
                }
            }
        }
        transactions.add(transaction)
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

fun List<Lot>.findLotsForId(id: LotIdentifier): List<Lot> {
    return when (id) {
        is LotIdentifier.DateLotIdentifier -> {
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

sealed interface TransformedFrom {
    data class WashSale(
        val originalLot: LotIdentifier,
        val fromTransaction: TransactionId,
    ) : TransformedFrom
}


