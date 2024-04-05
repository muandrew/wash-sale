package com.muandrew.stock.cli

import com.muandrew.stock.model.Transaction
import com.muandrew.stock.world.MoshiExt.addStockAdapters
import com.muandrew.stock.world.StockTransactionReader
import com.muandrew.stock.world.World
import com.muandrew.stock.world.generate1099Report
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

object StockCli {

    @JvmStatic
    fun main(args: Array<String>) {
        val ts = readTransactions(args[0])
        val outputDirectory = args.getOrNull(1)
        val w = World()
        println("processing transactions")
        w.sortAndProcessTransaction(ts)
        if (outputDirectory != null) {
            println("printing output")
            w.events.generate1099Report("$outputDirectory/1099.tsv")
        }
        println("end")
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun readTransactions(inputDirectory: String): List<Transaction> {
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
        return ts
    }

    internal fun readJsonIndexFile(jsonIndexFile: String): List<Transaction> {
        return StockTransactionReader.readTransactions(jsonIndexFile)
    }

    fun World.sortAndProcessTransaction(transactions: List<Transaction>) {
        val rs = transactions.filterIsInstance<Transaction.ReleaseTransaction>().sortedBy { it.date }
        val ss = transactions.filterIsInstance<Transaction.SaleTransaction>().sortedBy { it.date }
        for (r in rs) {
            processTransaction(r)
        }
        ss.forEach {
            processTransaction(it)
        }
    }
}