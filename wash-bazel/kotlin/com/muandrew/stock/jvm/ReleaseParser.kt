package com.muandrew.stock.jvm

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

object ReleaseParser {

    fun parse(htmlFile: File): Any {
        val doc = Jsoup.parse(htmlFile)
        val rootDiv = doc.body().child(0)
        assert(rootDiv.tagName() == "div")

        val res = mutableListOf<Map<String, String>>()
        val itr = rootDiv.children().iterator()
        itr.consumeUntil { it.id() == "RSU - Activity_table_footnotes" }
        itr.consumeUntil { it.tagName() == "br" }
        itr.consumeUntil {
            val a = it
            val b = itr.next()
            val c = itr.next()
            val releaseRawData = parseReleaseTable(a, b, c)
            res.add(releaseRawData)
            val next = itr.next()
            assert(next.tagName() == "br")
            itr.next().tagName() != "br"
        }
        return res
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

    private const val TOTAL_VALUE_KEY = "Total Value"
    private const val KEY_SOLD_WITHHOLD_TOTAL_VALUE = "Sold/Withheld Total Value"
    private const val KEY_RELEASE_METHOD = "Release Method"
    private const val RELEASE_METHOD_WITHHOLD = "Withhold shares to cover taxes, receive remaining shares"
    private const val RELEASE_METHOD_SOLD = "Sell enough shares to cover taxes, receive balance as shares"
    private val RELEASE_METHOD_MAP = mapOf(
        RELEASE_METHOD_WITHHOLD to "w",
        RELEASE_METHOD_SOLD to "s",
    )
    private val totalValueRegex = "$TOTAL_VALUE_KEY: (\\$[\\d.,]+ USD)".toRegex()

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
        trItr.next() // this is header data
        val data = mutableMapOf<String, String>()
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
        assert(RELEASE_METHOD_MAP[data[KEY_RELEASE_METHOD]] != null) {
            "unknown release method '${data[KEY_RELEASE_METHOD]}'"
        }
        data[KEY_SOLD_WITHHOLD_TOTAL_VALUE] = totalValueMatch!!.groups[1]!!.value
        return data
    }
}