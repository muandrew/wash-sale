package com.muandrew.stock.model

import com.muandrew.money.Money
import com.muandrew.stock.time.DateTime
import com.squareup.moshi.JsonClass

/**
 * @param overrideDateForSalesCalculation sometimes a different date is used, in case of wash sale
 */
@JsonClass(generateAdapter = true)
class Lot(
    val runId: String,
    val date: DateTime,
    val initial: LotValue,
    var current: LotValue,
    val overrideDateForSalesCalculation: DateTime? = null,
    val transformed: TransformedFrom? = null,
) {
    val isReplacement get() = transformed != null

    @Transient
    var numberOfWashes = 0

    /**
     * Includes the initial transaction
     */
    internal val transactions: MutableList<TransactionReference> = mutableListOf()

    override fun toString(): String {
        return "{date: `$date`, value: ${current.value}, shares: ${current.shares}}"
    }

    /**
     * Will remove the appropriate amount of cost basis
     *
     * @return The amount of cost basis removed
     */
    fun removeShares(ref: TransactionReference, sharesToRemove: Long) : Money {
        if (sharesToRemove > current.shares) {
            throw IllegalStateException("shares are not expected to go below zero from $ref")
        }
        transactions.add(ref)
        val res = current.splitOut(sharesToRemove)
        current = res.remainder
        return res.split.value
    }

    companion object {
        fun create(
            runId: String,
            date: DateTime,
            initial: LotValue,
            sourceTransaction: TransactionReference,
            transformed: TransformedFrom? = null,
        ): Lot {
            val lot = Lot(
                runId = runId,
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
        val originalLot: LotReference,
        val fromTransaction: TransactionReference,
    ) : TransformedFrom
}