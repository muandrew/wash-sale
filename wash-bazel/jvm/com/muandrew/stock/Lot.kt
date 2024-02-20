package com.muandrew.stock

import com.muandrew.stock.time.DateTime

class Lot(
    val id: LotIdentifier,
    val date: DateTime,
    sourceTransaction: TransactionId,
    val initial: LotSnapshot,
    val transformed: TransformedFrom? = null,
) {
    var current: LotSnapshot = initial
    val isReplacement get() = transformed != null

    /**
     * Includes the initial transaction
     */
    private val transactions: MutableList<TransactionId> = mutableListOf()

    init {
        transactions.add(sourceTransaction)
    }

    override fun toString(): String {
        return "{id: `$id`, value: ${current.value}, shares: ${current.shares}}"
    }
}