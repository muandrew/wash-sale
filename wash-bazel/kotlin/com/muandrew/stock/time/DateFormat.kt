package com.muandrew.stock.time

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

object DateFormat {
    //01-Jan-2000
    val DMY = DateTimeFormatter.ofPattern("dd-MMM-yyyy")

    fun parseDMY(input: String) : LocalDate {
        return LocalDate.parse(input, DMY)
    }
}

object NuFormat {
    private val nf = NumberFormat.getNumberInstance(Locale.US)

    fun parseLong(input: String): Long = nf.parse(input).toLong()
}