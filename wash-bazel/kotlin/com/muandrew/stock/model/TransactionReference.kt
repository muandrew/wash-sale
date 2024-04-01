package com.muandrew.stock.model

import java.time.LocalDate

sealed interface TransactionReference {
    data class DateReference(val date: LocalDate) : TransactionReference
}