package com.muandrew.stock.model

import java.time.LocalDate

sealed interface LotReference {
    data class Date(
        val date: LocalDate
    ): LotReference
}