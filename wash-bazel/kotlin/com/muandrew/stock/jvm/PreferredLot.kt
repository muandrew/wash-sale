package com.muandrew.stock.jvm

import com.muandrew.stock.time.DateFormat
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

fun PreferredLotData.chooseBasedOnDisbursement(date: LocalDate, shares: Long): Int {
    val list = this[date] ?: throw IllegalStateException("expected there to be something")
    val found = list.filter { it.second == shares }
    if (found.size != 1) {
        throw IllegalStateException("can't find $date, $shares")
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
