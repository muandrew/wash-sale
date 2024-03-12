package com.muandrew.stock.model

import com.muandrew.money.Money
import com.muandrew.stock.time.DateTime
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Lot internal constructor(
    val id: LotIdentifier,
    val date: DateTime,
    val initial: ShareValue,
    var current: ShareValue,
    val transformed: TransformedFrom? = null,
) {
    val isReplacement get() = transformed != null

    /**
     * Includes the initial transaction
     */
    internal val transactions: MutableList<TransactionId> = mutableListOf()

    override fun toString(): String {
        return "{id: `$id`, value: ${current.value}, shares: ${current.shares}}"
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
        val res = current.splitOut(sharesToRemove)
        current = res.remainder
        return res.split.value
    }

    companion object {
        fun create(
            id: LotIdentifier,
            date: DateTime,
            initial: ShareValue,
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

sealed interface TransformedFrom {
    data class WashSale(
        val originalLot: LotIdentifier,
        val fromTransaction: TransactionId,
    ) : TransformedFrom
}