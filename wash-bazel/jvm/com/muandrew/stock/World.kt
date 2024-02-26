package com.muandrew.stock

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
            }
            is Transaction.SaleTransaction -> {
                val lots = lots.findLotsForId(transaction.lotId)
                if (lots.isEmpty()) {
                    throw IllegalStateException("couldn't find applicable lot(s) based on id: ${transaction.lotId}")
                }

                // if we can just transfer the costs
                val firstLot = lots[0]
                if (transaction.shares <= firstLot.current.shares) {
                    firstLot.logTransaction(transaction.id, transaction.shares)
                } else {
                    // we will need to split
                    var shares = transaction.shares
//                val valuePerShareWithRem = (transaction.value / transaction.shares)
                    var lotI = 0;
                    while (shares > 0) {
                        val operatingLot = lots[lotI]
                        val sharesFromOperatingLot = min(operatingLot.current.shares, shares)
                        operatingLot.logTransaction(transaction.id, sharesFromOperatingLot)
                        shares -= sharesFromOperatingLot
                        //TODO
                        //account for remaider when shares hit zero
                    }
                }
            }
        }
        transactions.add(transaction)
    }

    override fun toString(): String {
        return """
            {
                lots: $lots
            }
        """.trimIndent()
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
    )
}


