package com.muandrew.stock.cli

import com.muandrew.stock.model.Transaction
import com.muandrew.stock.world.StockTransactionReader
import com.muandrew.stock.world.World

object StockCli {

    @JvmStatic
    fun main(args: Array<String>) {
        val w = readJsonIndexFile(args[0])
        print(w)
    }

    fun readJsonIndexFile(jsonIndexFile: String): World {
        val ts = StockTransactionReader.readTransactions(jsonIndexFile)
        val w = World()

        val ss = ts.filterIsInstance<Transaction.SaleTransaction>().sortedBy { it.date.date }
        val rs = ts.filterIsInstance<Transaction.ReleaseTransaction>().sortedBy { it.date.date }

        for (r in rs) {
            w.acceptTransaction(r)
        }
        ss.forEach {
            w.acceptTransaction(it)
        }
        return w
    }
}