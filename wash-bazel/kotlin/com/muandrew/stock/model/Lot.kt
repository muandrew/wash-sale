package com.muandrew.stock.model

import com.muandrew.money.Money
import com.squareup.moshi.JsonClass
import java.time.LocalDate

/**
 * @param overrideDateForSalesCalculation sometimes a different date is used, in case of wash sale
 */
@JsonClass(generateAdapter = true)
data class Lot(
    val runId: String,
    val lot: Int,
    val date: LocalDate,
    val initial: LotValue,
    var current: LotValue,
    val overrideDateForSalesCalculation: LocalDate? = null,
    val transformed: TransformedFrom? = null,
) {
    val isReplacement get() = transformed != null

    @Transient
    var nextWashNumber = 1

    /**
     * Includes the initial transaction
     */
    val transactions: MutableList<TransactionReference> = mutableListOf()

    /**
     * Will remove the appropriate amount of cost basis
     *
     * @return The amount of cost basis removed
     */
    fun removeShares(ref: TransactionReference, sharesToRemove: Long): Money {
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
            date: LocalDate,
            initial: LotValue,
            sourceTransaction: TransactionReference,
            lot: Int = -1,
            transformed: TransformedFrom? = null,
        ): Lot {
            val res = Lot(
                runId = runId,
                lot = lot,
                date = date,
                initial = initial,
                current = initial,
                transformed = transformed,
            )
            res.transactions.add(sourceTransaction)
            return res
        }
    }
}

sealed interface TransformedFrom {
    data class WashSale(
        val originalLot: LotReference,
        val fromTransaction: TransactionReference,
    ) : TransformedFrom
}