package com.muandrew.stock.time

import java.time.LocalDate
import java.time.LocalTime

data class DateTime(
    val date: LocalDate,
    val time: LocalTime? = null
)