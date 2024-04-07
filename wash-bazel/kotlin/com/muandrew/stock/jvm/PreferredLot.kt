package com.muandrew.stock.jvm

import com.muandrew.csv.Csv
import com.muandrew.stock.jvm.StatementParser.parseToInt
import com.muandrew.stock.jvm.StatementParser.parseToLong
import com.muandrew.stock.jvm.StatementParser.toDate
import com.muandrew.stock.time.DateFormat
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import java.io.File
import java.time.LocalDate

/**
 * {
 *      "2000-01-31" (disbursementDate): {
 *          "1" (lotNumber): 100 (disbursedShares)
 *          "2" (lotNumber): 10 (disbursedShares)
 *      }
 *      ...
 * }
 */
typealias PreferredLotDataRaw = Map<String, Map<String, Long>>
typealias PreferredLotData = MutableMap<LocalDate, MutableList<LotToDisbursedShares>>
typealias LotToDisbursedShares = Pair<Int, Long>

private const val date = "Acquisition Date"
private const val lot = "Lot"
private const val shares = "Shares"

@OptIn(ExperimentalStdlibApi::class)
fun parsePreferredLotData(
    moshi: Moshi,
    baseFilePath: String,
    preferredLotTsv: File,
    preferredLotJson: File
): PreferredLotData? {
    if (preferredLotTsv.exists() && preferredLotJson.exists()) {
        throw IllegalStateException("$baseFilePath: just use json or tsv, don't use both")
    }
    if (preferredLotJson.exists()) {
        val preferredLotDataRaw = preferredLotJson.bufferedReader().use {
            moshi.adapter<PreferredLotDataRaw>().fromJson(it.readText())
        }
        return preferredLotDataRaw!!.toPreferredLotData()
    }
    if (preferredLotTsv.exists()) {
        val tsvData = Csv.read(preferredLotTsv.absolutePath) {
            delimiter = '\t'
        }
        return tsvData
            .groupingBy { it[date]!!.toDate() }
            .aggregate { key, accumulator: MutableList<Pair<Int, Long>>?, element, first ->
                val res = accumulator ?: mutableListOf()
                val lot = element[lot]!!.parseToInt()
                val shares = element[shares]!!.parseToLong()
                res.add(lot to shares)
                res
            }.toMutableMap()
    }
    return null
}

fun PreferredLotData.chooseBasedOnDisbursement(date: LocalDate, shares: Long): Int? {
    val list = this[date] ?: return null
    val found = list.filter { it.second == shares }
    if (found.isEmpty()) {
        return null
    }
    if (found.size > 1) {
        println("note there's some ambuigity")
    }
    val id = found[0].first
    list.remove(found[0])
    if (list.isEmpty()) {
        this.remove(date)
    }
    return id
}

fun PreferredLotDataRaw.toPreferredLotData(): PreferredLotData {
    val res = mutableMapOf<LocalDate, MutableList<LotToDisbursedShares>>()
    this.forEach { entry ->
        val pairs = mutableListOf<LotToDisbursedShares>()
        entry.value.entries.mapTo(pairs) { strLng ->
            strLng.key.toInt() to strLng.value
        }
        val value = pairs.sortedBy { it.first }.toMutableList()
        res[DateFormat.parseDMY(entry.key)] = value
    }
    return res
}
