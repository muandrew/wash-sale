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
     * Will remove the appropriate amount of cost basis
     *
     * @return The amount of cost basis removed
     */
    fun transactForBasis(transactionId: TransactionId, sharesToRemove: Long) : Money {
        if (sharesToRemove > current.shares) {
            throw IllegalStateException("shares are not expected to go below zero from $transactionId")
        }
        transactions.add(transactionId)
        if (sharesToRemove == current.shares) {
            val costBasisConsumed = current.costBasis
            current = LotSnapshot(0, Money.ZERO)
            return costBasisConsumed
        } else {
            val old = current
            val pricePerShareWithRem = old.costBasis / old.shares
            val costBasisConsumed = sharesToRemove * pricePerShareWithRem.res
            val new = LotSnapshot(
                shares = old.shares - sharesToRemove,
                costBasis = old.costBasis - costBasisConsumed,
            )
            current = new
            return costBasisConsumed
        }
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