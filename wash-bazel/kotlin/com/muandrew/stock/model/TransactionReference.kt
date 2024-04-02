package com.muandrew.stock.model

import java.time.LocalDate

data class TransactionReference(
    val date: LocalDate,
    val referenceNumber: String?,
)
