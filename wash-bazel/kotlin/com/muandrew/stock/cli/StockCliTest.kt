package com.muandrew.stock.cli

import com.muandrew.moshi.adapters.LocalDateAdapter
import com.muandrew.moshi.adapters.LocalTimeAdapter
import com.muandrew.stock.model.*
import com.muandrew.testtool.TestFiles
import com.muandrew.testtool.TestFiles.readAsFileToString
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.file.Paths

@OptIn(ExperimentalStdlibApi::class)
class StockCliTest {

    lateinit var testData: String
    val moshi: Moshi = Moshi.Builder()
        .add(LocalDateAdapter())
        .add(LocalTimeAdapter())
        .add(
            PolymorphicJsonAdapterFactory.of(TransactionReport::class.java, "type")
                .withSubtype(TransactionReport.SaleReport::class.java, "sale")
                .withSubtype(TransactionReport.ReceivedReport::class.java, "received")
        )
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Before
    fun setUp() {
        testData = TestFiles.testDirectoryPath("kotlin/com/muandrew/stock/cli/testdata")
    }

    @Test
    fun example() {
        val t = StockCli.readJsonIndexFile("$testData/example_input.json")
        val w = StockCli.createWorld(t)

        val out = Out(w.lots, w.events.filterIsInstance<TransactionReport.SaleReport>())
        val a = moshi.adapter<Out>()

        assertEquals(
            Paths.get(testData, "example_out.json").readAsFileToString().filterNot { it.isWhitespace() },
            a.toJson(out)
        )
    }

    @Test
    fun washAfter() {
        val t = StockCli.readJsonIndexFile("$testData/washafter_input.json")
        val w = StockCli.createWorld(t)

        val out = Out(w.lots, w.events.filterIsInstance<TransactionReport.SaleReport>())
        val a = moshi.adapter<Out>()

        assertEquals(
            Paths.get(testData, "washafter_out.json").readAsFileToString().filterNot { it.isWhitespace() },
            a.toJson(out)
        )
    }

    data class Out(val lots: List<Lot>, val events: List<TransactionReport>)
}