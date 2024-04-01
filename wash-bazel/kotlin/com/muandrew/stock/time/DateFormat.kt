package com.muandrew.stock.time

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

object DateFormat {
    //01-Jan-2000
    val DMY = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
}

object NuFormat {
    private val nf = NumberFormat.getNumberInstance(Locale.US)

    fun parseLong(input: String): Long = nf.parse(input).toLong()
}