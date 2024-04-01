package com.muandrew.stock.cli

import com.muandrew.stock.model.Transaction
import com.muandrew.stock.world.MoshiExt.addStockAdapters
import com.muandrew.stock.world.StockTransactionReader
import com.muandrew.stock.world.World
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

object StockCli {

    @JvmStatic
    fun main(args: Array<String>) {
        val w = readTransactions(args[0])
        print(w)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun readTransactions(inputDirectory: String): World {
        val moshi = Moshi.Builder()
            .addStockAdapters()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val transactionAdapter = moshi.adapter<List<Transaction>>()

        val ts = mutableListOf<Transaction>()

        val files = File(inputDirectory).listFiles { _, fileName -> fileName.endsWith(".json") }
        files!!.forEach { file ->
            val res = file.bufferedReader().use {
                val fileData = it.readText()
                transactionAdapter.fromJson(fileData)
            }
            ts.addAll(res!!)
        }
        val w = World()
        w.sortAndProcessTransaction(ts)
        return w
    }

    fun readJsonIndexFile(jsonIndexFile: String): World {
        val ts = StockTransactionReader.readTransactions(jsonIndexFile)
        val w = World()
        w.sortAndProcessTransaction(ts)
        return w
    }

    private fun World.sortAndProcessTransaction(transactions: List<Transaction>) {
        val ss = transactions.filterIsInstance<Transaction.SaleTransaction>().sortedBy { it.date.date }
        val rs = transactions.filterIsInstance<Transaction.ReleaseTransaction>().sortedBy { it.date.date }
        for (r in rs) {
            processTransaction(r)
        }
        ss.forEach {
            processTransaction(it)
        }
    }
}