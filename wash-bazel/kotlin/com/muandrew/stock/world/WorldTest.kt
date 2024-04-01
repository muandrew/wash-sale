package com.muandrew.stock.world

import com.muandrew.money.Money
import com.muandrew.stock.model.*
import com.muandrew.stock.world.MoshiExt.addStockAdapters
import com.muandrew.stock.model.Transaction.ReleaseTransaction
import com.muandrew.stock.model.Transaction.SaleTransaction
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
        testDataDir = TestFiles.testDirectoryPath("kotlin/com/muandrew/stock/world/testdata")
    }

    @Test
    fun releaseTest() {
        val w = World()

        w.release(
            "2000-01-01",
            10,
            10_000
        )

        assertLots("release.json", w.lots)
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

        assertLots("sale.json", w.lots)
    }

    @Test
    fun testWashSale() {
        val w = World()

        val releaseShares = 100L
        val releaseValuePerShare = 2_00L
        w.release(
            "1999-01-01",
            releaseShares,
            releaseShares * releaseValuePerShare,
        )
        w.release(
            "2000-01-01",
            5,
            100_00
        )
        w.release(
            "2000-01-02",
            5,
            200_00
        )
        val saleTotalShares = 11L
        w.sale(
            "2000-01-03",
            saleTotalShares,
            11_00,
            "1999-01-01",
        )

        val wash = w.events.filterIsInstance<TransactionReport.SaleReport>().first()
        assertEquals(1, wash.allowedTransfer.sumOf { it.shares })
        assertEquals(Money(-1_00), wash.saleValue - wash.basisBeforeAdjustment - wash.disallowedValue)
        assertEquals(10, wash.disallowedTransfer.sumOf { it.shares })
        assertEquals(Money(-10_00), wash.disallowedValue)
        assertEquals(saleTotalShares, wash.shares)
    }

    private fun assertLots(recordingFile: String, actual: List<Lot>) {
        val expected = Paths.get(testDataDir, recordingFile).readAsFileToString().filterNot { it.isWhitespace() }
        val res = moshi.adapter<List<Lot>>().toJson(actual)
        assertEquals(expected, res)
    }
}

fun World.release(date: String, shares: Long, value: Long) {
    processTransaction(
        ReleaseTransaction(
            date = LocalDate.parse(date),
            disbursed = LotValue(shares, Money(value)),
        )
    )
}

fun World.sale(
    date: String,
    shares: Long,
    value: Long,
    lotDate: String
) {
    processTransaction(
        SaleTransaction(
            date = LocalDate.parse(date),
            value = Money(value),
            shares = shares,
            lotId = LotReference.Date(
                date = LocalDate.parse(lotDate)
            )
        )
    )
}