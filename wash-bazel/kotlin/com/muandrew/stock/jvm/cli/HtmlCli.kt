package com.muandrew.stock.jvm.cli

import com.muandrew.stock.jvm.PreferredLotDataRaw
import com.muandrew.stock.jvm.RealtimeTransaction
import com.muandrew.stock.jvm.StatementParser
import com.muandrew.stock.jvm.StatementParser.readRealtimeTransaction
import com.muandrew.stock.jvm.toPreferredLotData
import com.muandrew.stock.model.*
import com.muandrew.stock.model.Transaction.ReleaseTransaction
import com.muandrew.stock.model.Transaction.SaleTransaction
import com.muandrew.stock.time.NuFormat
import com.muandrew.stock.world.MoshiExt.addStockAdapters
import com.muandrew.stock.world.World
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

        val listFiles = fromFolder.listFiles { _, name -> name.endsWith(".html") && name != "espp.html" }

        val moshi = Moshi.Builder()
            .addStockAdapters()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val transactionAdapter = moshi.adapter<List<Transaction>>()

        val esppFile = File("$fromFolder/espp.html")
        if (esppFile.exists()) {
            val esppData = StatementParser.parseEspp(esppFile)
            val transactions = esppData.map { esppRelease ->
                ReleaseTransaction(
                    date = esppRelease.purchaseDate,
                    disbursed = LotValue(
                        shares = esppRelease.quantity,
                        value = esppRelease.totalPurchasePrice
                    ),
                    referenceNumber = "espp:${esppRelease.purchaseDate}"
                )
            }
            val outFile = File("$destFolder/espp.json")
            outFile.writeText(transactionAdapter.toJson(transactions))
        }

        listFiles.forEach { htmlFile ->
            val baseFilePath = htmlFile.absolutePath.removeSuffix(".html")
            val preferredLot = File("$baseFilePath.preflot.json")
            val saleData = fromFolder.listFiles()!!.filter {
                it.name.endsWith(".json")
                it.name.startsWith("${htmlFile.name.removeSuffix(".html")}.sale")
            }
            val rtMap = mutableMapOf<String, RealtimeTransaction>()
            saleData.map { moshi.readRealtimeTransaction(it) }.forEach {
                rtMap.put(it.listItem.referenceNumber, it)
            }
            val preferredLotDataRaw: PreferredLotDataRaw? = if (preferredLot.exists()) {
                preferredLot.bufferedReader().use {
                    moshi.adapter<PreferredLotDataRaw>().fromJson(it.readText())
                }
            } else null

            val statementData = StatementParser.parseStatementReport(
                htmlFile,
                preferredLotDataRaw?.toPreferredLotData()
            )

            val transactions = mutableListOf<Transaction>()
            val releaseTransactions: List<Transaction> = statementData.releaseTransactions.flatMap { realTransaction ->
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
                                lotId = LotReference(
                                    date = realTransaction.date,
                                    lotId = realTransaction.preferredLot
                                )
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
            transactions.addAll(releaseTransactions)

            statementData.partialWithdrawData.forEach { partialWithdrawData ->
                val realtimeTransaction = rtMap[partialWithdrawData.referenceNumber]!!
                val lotsSold = mutableListOf<RealtimeTransaction.CostBasis.TermedBasis.Lot>()
                realtimeTransaction.costBasis.longTerm?.rows?.let { lot ->
                    lotsSold.addAll(lot)
                }
                realtimeTransaction.costBasis.shortTerm?.rows?.let { lot ->
                    lotsSold.addAll(lot)
                }
                lotsSold.sortBy { it.lot }
                lotsSold.sortBy { it.purchaseDate }

                var lv = LotValue(
                    partialWithdrawData.sharesSold,
                    partialWithdrawData.netProceeds,
                )
                lotsSold.forEach { lot ->
                    val q = NuFormat.parseLong(lot.quantity)
                    val (split, rem) = lv.splitOut(q)
                    lv = rem
                    transactions.add(
                        SaleTransaction(
                            date = partialWithdrawData.settlementDate,
                            value = split.value,
                            shares = q,
                            lotId = LotReference(lot.purchaseDate, lot.lot),
                            referenceNumber = partialWithdrawData.referenceNumber,
                        )
                    )
                }
            }

            val outFile = File("$destFolder/${htmlFile.name.removeSuffix(".html")}.json")
            outFile.writeText(transactionAdapter.toJson(transactions))
        }
    }
}