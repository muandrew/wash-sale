package com.muandrew.stock

import com.muandrew.money.Money
import com.muandrew.money.times
import com.muandrew.stock.time.DateTime
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Lot internal constructor(
    val id: LotIdentifier,
    val date: DateTime,
    val initial: LotSnapshot,
    var current: LotSnapshot,
    val transformed: TransformedFrom? = null,
) {
    val isReplacement get() = transformed != null

    /**
     * Includes the initial transaction
     */
    internal val transactions: MutableList<TransactionId> = mutableListOf()

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
                costBasis = old.costBasis - (sharesToRemove * pricePerShareWithRem.res),
            )
            current = new
        }
        transactions.add(transactionId)
    }

    companion object {
        fun create(
            id: LotIdentifier,
            date: DateTime,
            initial: LotSnapshot,
            sourceTransaction: TransactionId,
            transformed: TransformedFrom? = null,
        ): Lot {
            val lot = Lot(
                id = id,
                date = date,
                initial = initial,
                current = initial,
                transformed = transformed,
            )
            lot.transactions.add(sourceTransaction)
            return lot
        }
    }
}