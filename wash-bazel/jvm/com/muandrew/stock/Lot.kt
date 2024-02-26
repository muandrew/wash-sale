package com.muandrew.stock

import com.muandrew.money.Money
import com.muandrew.money.times
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
        return "{id: `$id`, value: ${current.costBasis}, shares: ${current.shares}}"
    }

    /**
     * Will remove the approporate amount of cost basis
     */
    fun logTransaction(transactionId: TransactionId, sharesToRemove: Long) {
        if (sharesToRemove > current.shares) {
            throw IllegalStateException("shares are not expected to go below zero from $transactionId")
        }
        if (sharesToRemove == current.shares) {
            current = LotSnapshot(0, Money.ZERO)
        } else {
            val old = current
            val pricePerShareWithRem = old.costBasis / old.shares
            val new = LotSnapshot(
                shares = old.shares - sharesToRemove,
                costBasis =  old.costBasis - (sharesToRemove * pricePerShareWithRem.res),
            )
            current = new
        }
        transactions.add(transactionId)
    }
}