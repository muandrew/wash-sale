package com.muandrew.stock.model

import java.time.LocalDate

data class LotReference(
    val date: LocalDate,
    val lotId: Int? = null,
)