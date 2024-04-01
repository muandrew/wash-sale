package com.muandrew.stock.jvm.cli

import com.muandrew.stock.jvm.ReleaseParser
import com.muandrew.stock.model.LotReference
import com.muandrew.stock.model.LotValue
import com.muandrew.stock.model.RealTransaction
import com.muandrew.stock.model.Transaction
import com.muandrew.stock.model.Transaction.ReleaseTransaction
import com.muandrew.stock.model.Transaction.SaleTransaction
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
            val realTransactions = ReleaseParser.parse(htmlFile)
            val transactions: List<Transaction> = realTransactions.flatMap { realTransaction ->
                when (realTransaction) {
                    is RealTransaction.ReleaseSold -> {
                        listOf(
                            ReleaseTransaction(
                                referenceNumber = realTransaction.referenceNumber,
                                date = realTransaction.date,
                                disbursed = LotValue(
                                    realTransaction.gross.shares,
                                    realTransaction.gross.value
                                ),
                            ),
                            SaleTransaction(
                                referenceNumber = realTransaction.referenceNumber,
                                date = realTransaction.date,
                                value = realTransaction.sold.value,
                                shares = realTransaction.sold.shares,
                                lotId = LotReference.Date(date = realTransaction.date)
                            )
                        )
                    }

                    is RealTransaction.ReleaseWithheld -> {
                        listOf(
                            ReleaseTransaction(
                                referenceNumber = realTransaction.referenceNumber,
                                date = realTransaction.date,
                                disbursed = LotValue(
                                    realTransaction.disbursed.shares,
                                    realTransaction.disbursed.value
                                ),
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