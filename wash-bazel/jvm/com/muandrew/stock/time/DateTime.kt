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

operator fun DateTime.compareTo(other: DateTime): Int {
    val dateCompare = this.date.compareTo(other.date)
    return if (dateCompare != 0) {
        dateCompare
    } else { // date is same, now check time
        if (this.time == null && other.time != null) {
            return -1
        } else if (this.time != null && other.time == null) {
            return 1
        } else if (this.time != null && other.time != null) {
            return this.time.compareTo(other.time)
        } else {
            return 0
        }
    }
}