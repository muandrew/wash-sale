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

fun max(lhs: DateTime, rhs: DateTime): DateTime {
    return if (lhs > rhs) {
        lhs
    } else {
        rhs
    }
}

fun max(lhs: LocalDate, rhs: LocalDate): LocalDate {
    return if (lhs > rhs) {
        lhs
    } else {
        rhs
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

operator fun LocalDate.compareTo(other: LocalDate): Int {
    return if (isBefore(other)) {
        -1
    } else if (isAfter(other)) {
        1
    } else {
        0
    }
}