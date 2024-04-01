package com.muandrew.stock

import com.muandrew.stock.cli.StockCli
import com.muandrew.stock.world.World

object Wash {
    fun create(): World {
        val w = StockCli.readTransactions(
            ""
        )
        return w
    }
}