package com.muandrew.stock.model

import com.squareup.moshi.Json

data class RawInput(
    val value:  Map<String, String>?,
    val values: List<RawInput>?,
    @Json(name = "values_csv") val valuesCsv: List<String>?,
    )
