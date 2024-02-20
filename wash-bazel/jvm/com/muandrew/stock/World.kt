package com.muandrew.stock

class World {

    val transactions: MutableList<Transaction> = mutableListOf()
    val lots: MutableList<Lot> = mutableListOf()

    fun acceptTransaction(transaction: Transaction) {
        transactions.add(transaction)
        when (transaction) {
            is Transaction.ReleaseTransaction -> {
                lots.add(
                    Lot(
                        id = LotIdentifier.DateLotIdentifier(transaction.date),
                        date = transaction.date,
                        initial = LotSnapshot(
                            shares = transaction.shares,
                            value = transaction.value,
                        ),
                        sourceTransaction = transaction.id
                    )
                )
            }
            is Transaction.SaleTransaction -> {
                //TODO
            }
        }
    }

    override fun toString(): String {
        return """
            {
                lots: $lots
            }
        """.trimIndent()
    }
}

sealed interface TransformedFrom {
    data class WashSale(
        val originalLot: LotIdentifier,
        val fromTransaction: TransactionId,
    )
}


