package com.muandrew.stock.world

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.muandrew.money.Money
import com.muandrew.stock.model.LotReference
import com.muandrew.stock.model.LotValue
import com.muandrew.stock.model.RawInput
import com.muandrew.stock.model.Transaction
import com.muandrew.stock.model.Transaction.ReleaseTransaction
import com.muandrew.stock.time.DateTime
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.time.LocalDate

object StockTransactionReader {

    private fun createMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun readFile(eventsFilePath: String): List<RawInput>? {
        val moshi = createMoshi()
        val a = moshi.adapter<List<RawInput>>()

        val f = File(eventsFilePath)
        val res = f.bufferedReader().use {
            val fileData = it.readText()
            a.fromJson(fileData)
        }
        return res
    }

    private fun readCsv(csvFilePath: String): List<Map<String, String>> {
        return csvReader().readAllWithHeader(File(csvFilePath))
    }

    private fun collectResult(
        rootDir: String,
        transactions: MutableList<Transaction>,
        defaultValues: Map<String, String>,
        data: List<RawInput>
    ) {
        data.forEach { rawInput ->
            val newDef = rawInput.value?.let { defaultValues.toMutableMap().apply { putAll(it) } } ?: defaultValues
            val valuesCsv = rawInput.valuesCsv
            val values = rawInput.values
            val value = rawInput.value
            if (valuesCsv != null) {
                valuesCsv.forEach { csvFile ->
                    val csvData = readCsv("$rootDir/$csvFile")
                    csvData.forEach { row ->
                        transactions.add(parseData(newDef, row))
                    }
                }
            } else if (values != null) {
                collectResult(rootDir, transactions, newDef, values)
            } else if (value != null) {
                transactions.add(parseData(defaultValues, defaultValues))
            } else {
                throw IllegalArgumentException("need values or values_csv")
            }
        }
    }

    private fun getData(default: Map<String, String>, instance: Map<String, String>, key: String): String? {
        return (instance[key] ?: default[key])?.trim()
    }

    private fun parseData(default: Map<String, String>, instance: Map<String, String>): Transaction {
        val type = getData(default, instance, "event_type") ?: IllegalStateException("need event_type, release|sale")
        return when (type) {
            "release" -> {
                ReleaseTransaction(
                    date = LocalDate.parse(getData(default, instance, "date")!!),
                    disbursed = LotValue(
                        shares = getData(default, instance, "shares")!!.toLong(),
                        value = Money.parse(getData(default, instance, "value")!!)
                    ),
                )
            }

            "sale" -> {
                val date = LocalDate.parse(getData(default, instance, "date")!!)
                val lotDate = LocalDate.parse(getData(default, instance, "sale_lot")!!)
                Transaction.SaleTransaction(
                    date,
                    Money.parse(getData(default, instance, "value")!!),
                    getData(default, instance, "shares")!!.toLong(),
                    LotReference(date = lotDate),
                )
            }

            else -> {
                throw IllegalStateException("something unexpected $type")
            }
        }
    }

    fun readTransactions(eventsFilePath: String): List<Transaction> {
        val rawFile = readFile(eventsFilePath)!!
        val res = mutableListOf<Transaction>()
        collectResult(File(eventsFilePath).parent, res, mutableMapOf(), rawFile)
        return res
    }
}