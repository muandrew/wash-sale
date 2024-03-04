package com.muandrew.stock.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class DateTime(
    val date: LocalDate,
    val time: LocalTime? = null
) {
    override fun toString(): String {
        if (time != null) {
            return LocalDateTime.of(date, time).toString()
        }
        return date.toString()
    }
}