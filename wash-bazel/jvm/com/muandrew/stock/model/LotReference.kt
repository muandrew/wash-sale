package com.muandrew.stock.model

import com.muandrew.stock.time.DateTime

sealed interface LotReference {
    data class DateLotReference(
        val date: DateTime
    ): LotReference
}