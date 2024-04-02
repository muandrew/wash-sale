package com.muandrew.stock.jvm

import com.muandrew.money.Money
import com.muandrew.stock.model.LotValue
import com.muandrew.stock.model.RealTransaction
import com.muandrew.stock.time.DateFormat
import com.muandrew.stock.time.NuFormat
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.time.LocalDate

object StatementParser {

    data class StatementData(
        val releaseTransactions: List<RealTransaction>,
        val partialWithdrawData: List<PartialWithdarawData>,
    )

    fun parse(
        htmlFile: File,
        preferredLotData: PreferredLotData? = null
    ): StatementData {
        val rawData = parseRaw(htmlFile)
        val transactions = rawData.release
            .map { it.asRealTransaction(preferredLotData) }
            .sortWithDateAndLot(preferredLotData)
        return StatementData(
            releaseTransactions = transactions,
            partialWithdrawData = rawData.withdraw.map { it.toPartialWithdarawData() }
        )
    }

    private fun List<RealTransaction>.sortWithDateAndLot(
        preferredLotData: PreferredLotData?
    ): List<RealTransaction> {
        return if (preferredLotData != null) {
            sortedWith { a, b ->
                val dateComparison = a.date.compareTo(b.date)
                if (dateComparison == 0) {
                    a.preferredLot!!.compareTo(b.preferredLot!!)
                } else {
                    dateComparison
                }
            }
        } else {
            this
        }
    }

    private fun List<RealTransaction>.sortWithGrant(): List<RealTransaction> {
        return sortedWith { a, b ->
            if (a.date > b.date) {
                1
            } else if (a.date < b.date) {
                -1
            } else {
                if (a.grantDate > b.date) {
                    1
                } else if (a.grantDate < b.grantDate) {
                    -1
                } else {
                    0
                }
            }
        }
    }

    internal fun Map<String, String>.asRealTransaction(
        preferredLotData: PreferredLotData? = null
    ): RealTransaction {
        val date = releaseDate()
        val grossValue = grossValue()
        val soldWithheldValue = soldOrWithheldValue()
        val releasePrice = releasePrice()
        val ref = this[KEY_REFERENCE_NUMBER]!!
        val grantDate = asDate("Grant Date")
        val disbursedShares = disbursedShares()
        return when (releaseMethod()) {
            ReleaseMethod.WITHHELD -> RealTransaction.ReleaseWithheld(
                referenceNumber = ref,
                date = date,
                grantDate = grantDate,
                preferredLot = preferredLotData?.chooseBasedOnDisbursement(date, disbursedShares),
                gross = LotValue(
                    shares = releasedShares(),
                    value = grossValue,
                ),
                disbursed = LotValue(
                    shares = disbursedShares,
                    value = grossValue - soldWithheldValue,
                ),
                withheld = LotValue(
                    shares = asLong("Number of Restricted Awards Withheld"),
                    value = soldWithheldValue,
                ),
                releasePrice = releasePrice,
            )

            ReleaseMethod.SOLD -> RealTransaction.ReleaseSold(
                referenceNumber = ref,
                date = date,
                grantDate = grantDate,
                preferredLot = preferredLotData?.chooseBasedOnDisbursement(date, disbursedShares),
                gross = LotValue(
                    shares = releasedShares(),
                    value = grossValue,
                ),
                disbursed = LotValue(
                    shares = disbursedShares,
                    value = grossValue,
                ),
                sold = LotValue(
                    shares = asLong("Number of Restricted Awards Sold"),
                    value = soldWithheldValue,
                ),
                releasePrice = releasePrice,
                salePrice = salePrice(),
            )
        }
    }

    data class StatementRaw(
        val release: List<Map<String, String>>,
        val withdraw: List<Map<String, String>>
    )

    fun parseRaw(htmlFile: File): StatementRaw {
        val doc = Jsoup.parse(htmlFile)
        val rootDiv = doc.body().child(0)
        assert(rootDiv.tagName() == "div")

        val releasesRaw = mutableListOf<Map<String, String>>()
        val withdrawsRaw = mutableListOf<Map<String, String>>()
        val itr = rootDiv.children().iterator()
        itr.consumeUntil { it.id() == "RSU - Activity_table_footnotes" }
        itr.consumeUntil { it.tagName() == "br" }
        itr.consumeUntil {
            val a = it
            val b = itr.next()
            val c = itr.next()
            val releaseRawData = parseReleaseTable(a, b, c)
            releasesRaw.add(releaseRawData)
            val next = itr.next()
            assert(next.tagName() == "br")
            itr.next().tagName() != "br"
        }
        itr.consumeUntil { it.id() == "Activity_table_footnotes" }
        itr.consumeUntil { it.tagName() == "br" }
        itr.consumeUntil {
            val a = it
            val b = itr.next()
            val c = itr.next()
            val withdrawRawData = parseWithdrawTable(a, b, c)
            withdrawsRaw.add(withdrawRawData)
            val next = itr.next()
            assert(next.tagName() == "br")
            itr.next().tagName() != "br"
        }
        return StatementRaw(releasesRaw, withdrawsRaw)
    }

    private fun <T> MutableIterator<T>.consumeUntil(predicate: (T) -> Boolean) {
        // should crash if predicate isn't hit before end of iterator
        while (true) {
            val node = next()
            if (predicate(node)) {
                return
            }
        }
    }

    private enum class ReleaseMethod {
        SOLD,
        WITHHELD,
    }

    private const val KEY_SOLD_WITHHOLD_TOTAL_VALUE = "Sold/Withheld Total Value"
    private const val KEY_RELEASE_METHOD = "Release Method"
    private const val RELEASE_METHOD_WITHHOLD = "Withhold shares to cover taxes, receive remaining shares"
    private const val RELEASE_METHOD_SOLD = "Sell enough shares to cover taxes, receive balance as shares"
    private val RELEASE_METHOD_MAP = mapOf(
        RELEASE_METHOD_WITHHOLD to ReleaseMethod.WITHHELD,
        RELEASE_METHOD_SOLD to ReleaseMethod.SOLD,
    )
    private val totalValueRegex = "Total Value: (\\$[\\d.,]+ USD)".toRegex()
    private val referenceRegex = "\\((.*)\\)".toRegex()
    private const val KEY_REFERENCE_NUMBER = "Reference Number"

    private fun Map<String, String>.releaseMethod(): ReleaseMethod {
        val method = RELEASE_METHOD_MAP[this[KEY_RELEASE_METHOD]]
        assert(method != null) {
            "unknown release method '${this[KEY_RELEASE_METHOD]}'"
        }
        //TODO assert non null with contract
        return method!!
    }

    private fun Map<String, String>.releaseDate() =
        asDate("Release Date")

    internal fun Map<String, String>.asDate(key: String) =
        LocalDate.parse(this[key], DateFormat.DMY)

    private fun Map<String, String>.releasedShares() =
        asLong("Number of Restricted Awards Released")

    private fun Map<String, String>.grossValue() =
        asMoney("Gross Release Value")

    private fun Map<String, String>.disbursedShares() =
        asLong("Number of Restricted Awards Disbursed")

    private fun Map<String, String>.soldOrWithheldValue() =
        asMoney("Sold/Withheld Total Value")

    private fun Map<String, String>.releasePrice() =
        asMoney("Release Price")

    private fun Map<String, String>.salePrice() =
        asMoney("Sale Price")

    internal fun Map<String, String>.asLong(key: String): Long =
        NuFormat.parseLong(this[key]!!)

    internal fun Map<String, String>.asMoney(key: String): Money =
        Money.parse(this[key]!!.removeSuffix(" USD").replace(",", ""))


    internal fun parseReleaseTable(
        releaseTable: Element,
        valueSoldOrWithheldSubtotal: Element,
        totalValueTable: Element,
    ): Map<String, String> {
        val tbodies = releaseTable.getElementsByTag("tbody")
        assert(tbodies.size == 1)
        val tbody = tbodies[0]
        val trs = tbody.getElementsByTag("tr")
        assert(trs.size == 8 || trs.size == 9)
        val trItr = trs.iterator()
        val data = mutableMapOf<String, String>()
        val header = trItr.next() // this is header data
        data[KEY_REFERENCE_NUMBER] = referenceRegex.find(header.text())!!.groups[1]!!.value
        while (trItr.hasNext()) {
            val tr = trItr.next()
            val tds = tr.getElementsByTag("td")
            assert(tds.size == 4)
            data[tds[0].text().trim().removeSuffix(":")] = tds[1].text().trim()
            data[tds[2].text().trim().removeSuffix(":")] = tds[3].text().trim()
        }
        // parsing out total value
        val totalValueTds = totalValueTable.getElementsByTag("td")
        assert(totalValueTds.size == 1)
        val totalValueMatch = totalValueRegex.find(totalValueTds[0].text())
        data[KEY_SOLD_WITHHOLD_TOTAL_VALUE] = totalValueMatch!!.groups[1]!!.value
        return data
    }

    private const val KEY_NET_PROCEEDS = "Net Proceeds"
    private val netProceedsRegex = "$KEY_NET_PROCEEDS: (\\$[\\d.,]+ USD)".toRegex()

    internal fun parseWithdrawTable(
        withdraw: Element,
        saleBreakdown: Element,
        netProceeds: Element,
    ): Map<String, String> {
        val tbodies = withdraw.getElementsByTag("tbody")
        assert(tbodies.size == 1)
        val tbody = tbodies[0]
        val trs = tbody.getElementsByTag("tr")
        assert(trs.size == 6)
        val trItr = trs.iterator()
        val data = mutableMapOf<String, String>()
        trItr.next() // this is header data, its redundant
        while (trItr.hasNext()) {
            val tr = trItr.next()
            val tds = tr.getElementsByTag("td")
            assert(tds.size == 4)
            data[tds[0].text().trim().removeSuffix(":")] = tds[1].text().trim()
            data[tds[2].text().trim().removeSuffix(":")] = tds[3].text().trim()
        }
        // parsing out net proceeds
        val totalValueTds = netProceeds.getElementsByTag("td")
        assert(totalValueTds.size == 1)
        val totalValueMatch = netProceedsRegex.find(totalValueTds[0].text())
        data[KEY_NET_PROCEEDS] = totalValueMatch!!.groups[1]!!.value
        return data
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun Moshi.readRealtimeTransaction(it: File): RealtimeTransaction {
        val rtAdapter = adapter<RealtimeTransaction>()
        return it.bufferedReader().use {
            rtAdapter.fromJson(it.readText())!!
        }
    }
}