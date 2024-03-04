package com.muandrew.stock

import com.muandrew.money.Money
import kotlin.math.min

class World {

    val transactions: MutableList<Transaction> = mutableListOf()
    val lots: MutableList<Lot> = mutableListOf()

    fun acceptTransaction(transaction: Transaction) {
        when (transaction) {
            is Transaction.ReleaseTransaction -> {
                lots.add(
                    Lot.create(
                        id = LotIdentifier.DateLotIdentifier(transaction.date),
                        date = transaction.date,
                        initial = LotSnapshot(
                            shares = transaction.shares,
                            costBasis = transaction.value,
                        ),
                        sourceTransaction = transaction.id
                    )
                )
                val valuePerShareWithRem = transaction.value / transaction.shares
                println("${transaction.date}: rcv ${transaction.shares} share(s) totalling ${transaction.value}. [${valuePerShareWithRem.res} per share]")
            }

            is Transaction.SaleTransaction -> {
                val lots = lots.findLotsForId(transaction.lotId)
                if (lots.isEmpty()) {
                    throw IllegalStateException("couldn't find applicable lot(s) based on id: ${transaction.lotId}")
                }

                // if we can just transfer the costs
                val firstLot = lots[0]
                val costBasis = if (transaction.shares <= firstLot.current.shares) {
                    firstLot.transactForBasis(transaction.id, transaction.shares)
                } else {
                    // we will need to split
                    var shares = transaction.shares
                    var lotI = 0;
                    var costBasis = Money.ZERO
                    while (shares > 0) {
                        val operatingLot = lots[lotI]
                        val sharesFromOperatingLot = min(operatingLot.current.shares, shares)
                        costBasis += operatingLot.transactForBasis(transaction.id, sharesFromOperatingLot)
                        shares -= sharesFromOperatingLot
                    }
                    Money.ZERO
                }
                val net = transaction.value - costBasis
                println("${transaction.date}: sld ${transaction.shares} share(s) for ${transaction.value} against cost basis of $costBasis. [net: $net]")
                // check for wash sale
            }
        }
        transactions.add(transaction)
    }

    override fun toString(): String {
        return "{lots: $lots}"
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


