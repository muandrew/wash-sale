package com.muandrew.stock

import com.muandrew.money.Money
import com.muandrew.stock.MoshiExt.addStockAdapters
import com.muandrew.testtool.TestFiles
import com.muandrew.testtool.TestFiles.readAsFileToString
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.file.Paths
import java.time.LocalDate


@OptIn(ExperimentalStdlibApi::class)
class WorldTest {

    lateinit var testDataDir: String
    lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .addStockAdapters()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        testDataDir = TestFiles.testDirectoryPath("jvm/com/muandrew/stock/testdata")
    }

    @Test
    fun releaseTest() {
        val w = World()

        w.release(
            "2000-01-01",
            10,
            10_000
        )

        assertLots("1.json", w.lots)
    }

    @Test
    fun saleTest() {
        val w = World()

        w.release(
            "2000-01-01",
            10,
            10_000
        )
        w.sale(
            "2000-01-02",
            1,
            2_000,
            "2000-01-01",
        )

        assertLots("2.json", w.lots)
    }

    private fun assertLots(recordingFile: String, actual: List<Lot>) {
        val expected = Paths.get(testDataDir, recordingFile).readAsFileToString().filterNot { it.isWhitespace() }
        val res = moshi.adapter<List<Lot>>().toJson(actual)
        assertEquals(expected, res)
    }
}

fun World.release(date: String, shares: Long, value: Long) {
    acceptTransaction(Transaction.createRelease(LocalDate.parse(date), shares, Money(value)))
}

fun World.sale(
    date: String,
    shares: Long,
    value: Long,
    lotDate: String
) {
    acceptTransaction(
        Transaction.createSale(
            LocalDate.parse(date),
            shares,
            Money(value),
            LocalDate.parse(lotDate)
        )
    )
}