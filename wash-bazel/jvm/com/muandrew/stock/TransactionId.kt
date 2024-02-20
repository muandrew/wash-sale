package com.muandrew.stock

import com.muandrew.stock.time.DateTime

sealed interface TransactionId {
    data class DateId(val date: DateTime) : TransactionId
}