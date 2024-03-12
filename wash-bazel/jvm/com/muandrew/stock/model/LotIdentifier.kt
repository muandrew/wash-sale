package com.muandrew.stock.model

import com.muandrew.stock.time.DateTime

sealed interface LotIdentifier {
    data class DateLotIdentifier(
        val date: DateTime
    ): LotIdentifier
}