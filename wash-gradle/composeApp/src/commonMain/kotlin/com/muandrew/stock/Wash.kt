package com.muandrew.stock

import com.muandrew.stock.cli.StockCli
import com.muandrew.stock.cli.StockCli.sortAndProcessTransaction
import com.muandrew.stock.world.World

object Wash {
    fun create(): World {
        val transactions = StockCli.readTransactions(
            ""
        )
        val w = World()
        w.sortAndProcessTransaction(transactions)
        return w
    }
}