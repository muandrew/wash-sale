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
            PolymorphicJsonAdapterFactory.of(TransformedFrom::class.java, "type")
                .withSubtype(TransformedFrom.WashSale::class.java, "wash_sale")
        )
        .add(
            PolymorphicJsonAdapterFactory.of(LotIdentifier::class.java, "type")
                .withSubtype(LotIdentifier.DateLotIdentifier::class.java, "date")
        )
        .add(
            PolymorphicJsonAdapterFactory.of(TransactionId::class.java, "type")
                .withSubtype(TransactionId.DateId::class.java, "date")
        )
        .add(
            PolymorphicJsonAdapterFactory.of(ReportEvent::class.java, "type")
                .withSubtype(ReportEvent.SaleEvent::class.java, "sale")
                .withSubtype(ReportEvent.WashSaleEvent::class.java, "wash")
                .withSubtype(ReportEvent.ReceivedEvent::class.java, "received")
        )
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Before
    fun setUp() {
        testData = TestFiles.testDirectoryPath("jvm/com/muandrew/stock/cli/testdata")
    }

    @Test
    fun a() {
        val w = StockCli.read("$testData/example_input.json")

        val out = Out(w.lots, w.events.filter { it is ReportEvent.SaleEvent || it is ReportEvent.WashSaleEvent })
        val a = moshi.adapter<Out>()

        assertEquals(
            Paths.get(testData, "example_out.json").readAsFileToString().filterNot { it.isWhitespace() },
            a.toJson(out)
        )
    }

    @Test
    fun washAfter() {
        val w = StockCli.read("$testData/washafter_input.json")

        val out = Out(w.lots, w.events.filter { it is ReportEvent.SaleEvent || it is ReportEvent.WashSaleEvent })
        val a = moshi.adapter<Out>()

        assertEquals(
            Paths.get(testData, "washafter_out.json").readAsFileToString().filterNot { it.isWhitespace() },
            a.toJson(out)
        )
    }

    data class Out(val lots: List<Lot>, val events: List<ReportEvent>)
}