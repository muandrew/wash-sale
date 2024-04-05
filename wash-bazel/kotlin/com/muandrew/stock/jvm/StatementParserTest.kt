package com.muandrew.stock.jvm

import com.muandrew.stock.jvm.StatementParser.asRealTransaction
import com.muandrew.stock.jvm.StatementParser.parseEspp
import com.muandrew.stock.jvm.StatementParser.parseReleaseTable
import com.muandrew.testtool.TestFiles
import org.jsoup.Jsoup
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File

class StatementParserTest {

    lateinit var testData: String

    @Before
    fun setUp() {
        testData = TestFiles.testDirectoryPath("kotlin/com/muandrew/stock/jvm/testdata")
    }

    @Ignore
    @Test
    fun statementTest() {
        val file = File("$testData/year_statement.html")
        val res = StatementParser.parseStatementRaw(file)
        val transactions = StatementParser.parseStatementReport(file)

        assertNotNull(res)
        assertNotNull(transactions)
    }

    @Test
    fun releaseWithold() {
        val doc = Jsoup.parse(File("$testData/release_withheld.html"))
        val tables = doc.getElementsByTag("div")[0].children()
        val rawResult = parseReleaseTable(tables[0], tables[1], tables[2])
        val t = rawResult.asRealTransaction()

        assertNotNull(rawResult)
        assertNotNull(t)
    }

    @Test
    fun releaseSold() {
        val doc = Jsoup.parse(File("$testData/release_sold.html"))
        val tables = doc.getElementsByTag("div")[0].children()
        val rawResult = parseReleaseTable(tables[0], tables[1], tables[2])
        val t = rawResult.asRealTransaction()

        assertNotNull(rawResult)
        assertNotNull(t)
    }

    @Test
    fun esppHTML() {
        val res = parseEspp(File("$testData/espp.html"))

        assertNotNull(res)
    }
}