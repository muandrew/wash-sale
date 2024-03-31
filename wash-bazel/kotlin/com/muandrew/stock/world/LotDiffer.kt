package com.muandrew.stock.world

import com.muandrew.csv.Csv
import java.text.NumberFormat
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import java.util.*
import javax.swing.text.NumberFormatter

object LotDiffer {

    private data class Key(val date: LocalDate, val lotNumber: Int)

    val MDY = DateTimeFormatter.ofPattern("dd-MMM-yyyy")

    fun diff(initialCsvFile: String, finalCsvFile: String): List<LotDiff> {
        val ini = Csv.read(initialCsvFile) {
            delimiter = '\t'
        }
        val fin = Csv.read(finalCsvFile) {
            delimiter = '\t'
        }
        val numberFormat = NumberFormat.getNumberInstance(Locale.US)
        val res = differ(ini, fin,
            keySelector = {
                Key(date = LocalDate.parse(it[DATE],MDY), lotNumber = numberFormat.parse(it[LOT_NUMBER]!!).toInt())
            }, intermediateMapper = {
                numberFormat.parse(it[SHARES]!!).toLong()
            },
            { k, i, f ->
                val iniShares = i ?: 0
                val finalShares = f ?: 0
                val diff = finalShares - iniShares
                if (diff != 0L) {
                    LotDiff(
                        date = k.date,
                        lotNumber = k.lotNumber,
                        sharesDiff = diff
                    )
                } else {
                    null
                }
            })
        return res
    }

    private fun <T : Any, K : Any, I : Any, V : Any> differ(
        ini: List<T>,
        fin: List<T>,
        keySelector: (T) -> K,
        intermediateMapper: (T) -> I,
        valueMapper: (k: K, i: I?, f: I?) -> V?,
    ): List<V> {
        val iniMap = ini.toDiffMap(keySelector, intermediateMapper).toMutableMap()
        val finMap = fin.toDiffMap(keySelector, intermediateMapper)
        val res = mutableListOf<V>()
        finMap.forEach { finEntry ->
            valueMapper(finEntry.key, iniMap[finEntry.key], finEntry.value)?.let { diffRes -> res.add(diffRes) }
            iniMap.remove(finEntry.key)
        }
        iniMap.forEach {
            valueMapper(it.key, it.value, null)?.let { diffRes -> res.add(diffRes) }
        }
        return res
    }

    private fun <T, K, V> List<T>.toDiffMap(keySelector: (T) -> K, valueMapper: (T) -> V): Map<K, V> {
        return groupingBy { keySelector(it) }.aggregate { key, accumulator: V?, element, first ->
            if (!first) {
                throw IllegalStateException("duplicate key detected $key")
            }
            valueMapper(element)
        }
    }

    data class LotDiff(
        val date: LocalDate,
        val lotNumber: Int,
        val sharesDiff: Long
    )

    private const val DATE = "Acquisition Date"
    private const val LOT_NUMBER = "Lot"
    private const val SHARES = "Total Shares You Hold"
}