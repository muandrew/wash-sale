package com.muandrew.stock.model

import java.time.LocalDate

data class TransactionReference(
    val date: LocalDate,
    val referenceNumber: String?,
) : Comparable<TransactionReference> {

    override operator fun compareTo(other: TransactionReference): Int {
        val d = this.date.compareTo(date)
        return if (d == 0) {
            if (referenceNumber == null && other.referenceNumber == null) 0
            else if (referenceNumber == null && other.referenceNumber != null) 1
            else if (referenceNumber != null && other.referenceNumber == null) -1
            else referenceNumber!!.compareTo(other.referenceNumber!!)
        } else d
    }
}
