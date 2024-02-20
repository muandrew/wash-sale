package com.muandrew.stock

import com.muandrew.money.Money

data class LotSnapshot(
    val shares: Long,
    val value: Money,
)