package com.muandrew.stock

import com.muandrew.stock.cli.StockCli
import com.muandrew.stock.cli.StockCli.sortAndProcessTransaction
import com.muandrew.stock.world.World

object Wash {
    fun create(inputDirectory: String): World {
        val transactions = StockCli.readTransactions(inputDirectory)
        val w = World()
        w.sortAndProcessTransaction(transactions)
        return w
    }
}