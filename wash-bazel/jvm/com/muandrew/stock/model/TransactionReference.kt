package com.muandrew.stock.model

import com.muandrew.stock.time.DateTime

sealed interface TransactionReference {
    data class DateReference(val date: DateTime) : TransactionReference
}