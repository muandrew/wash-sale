package com.muandrew.stock.world

import com.muandrew.testtool.TestFiles
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class StockTransactionReaderTest {

    lateinit var testData: String

    @Before
    fun setUp() {
        testData = TestFiles.testDirectoryPath("kotlin/com/muandrew/stock/world/testdata")
    }

    @Test
    fun a() {

        val res = StockTransactionReader.readFile("$testData/test.json")

        assertNotNull(res)
    }

    @Test
    fun b() {

        val res = StockTransactionReader.readTransactions("$testData/test.json")

        assertNotNull(res)
    }
}