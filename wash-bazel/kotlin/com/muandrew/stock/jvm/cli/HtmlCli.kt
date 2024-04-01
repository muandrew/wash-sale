package com.muandrew.stock.jvm.cli

import com.muandrew.stock.jvm.ReleaseParser
import com.muandrew.stock.model.RealTransaction
import com.muandrew.stock.model.Transaction
import com.muandrew.stock.world.MoshiExt.addStockAdapters
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

object HtmlCli {

    @OptIn(ExperimentalStdlibApi::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val fromFolder = File(args[0])
        val destFolder = File(args[1])

        val listFiles = fromFolder.listFiles { _, name -> name.endsWith(".html") }

        val moshi = Moshi.Builder()
            .addStockAdapters()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val transactionAdapter = moshi.adapter<List<Transaction>>()

        listFiles.forEach { htmlFile ->
            val rt = ReleaseParser.parse(htmlFile)
            val transactions: List<Transaction> = rt.flatMap { realTransaction ->
                when (realTransaction) {
                    is RealTransaction.ReleaseSold -> {
                        listOf(
                            Transaction.createRelease(
                                realTransaction.date,
                                realTransaction.gross.shares,
                                realTransaction.gross.value,
                            ),
                            Transaction.createSale(
                                realTransaction.date,
                                realTransaction.sold.shares,
                                realTransaction.sold.value,
                                lotDate = realTransaction.date,
                            )
                        )
                    }

                    is RealTransaction.ReleaseWithheld -> {
                        listOf(
                            Transaction.createRelease(
                                realTransaction.date,
                                realTransaction.disbursed.shares,
                                realTransaction.disbursed.value,
                            ),
                        )
                    }

                    else -> {
                        throw IllegalStateException("unexpected arm")
                    }
                }
            }
            val outFile = File("$destFolder/${htmlFile.name.removeSuffix(".html")}.json")
            outFile.writeText(transactionAdapter.toJson(transactions))
        }
    }
}