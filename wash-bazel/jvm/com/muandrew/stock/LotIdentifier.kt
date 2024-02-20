package com.muandrew.stock

import com.muandrew.stock.time.DateTime

sealed interface LotIdentifier {
    data class DateLotIdentifier(
        val date: DateTime
    ): LotIdentifier
}