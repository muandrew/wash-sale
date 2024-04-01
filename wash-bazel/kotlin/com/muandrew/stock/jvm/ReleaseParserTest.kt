package com.muandrew.stock.jvm

import com.muandrew.stock.jvm.ReleaseParser.parseReleaseTable
import com.muandrew.testtool.TestFiles
import org.jsoup.Jsoup
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File

class ReleaseParserTest {

    lateinit var testData: String

    @Before
    fun setUp() {
        testData = TestFiles.testDirectoryPath("kotlin/com/muandrew/stock/jvm/testdata")
    }

    @Ignore
    @Test
    fun statementTest() {
        val res = ReleaseParser.parse(File("$testData/year_statement.html"))

        assertNotNull(res)
    }

    @Test
    fun releaseWithold() {
        val doc = Jsoup.parse(File("$testData/release_withheld.html"))
        val tables = doc.getElementsByTag("div")[0].children()
        val rawResult = parseReleaseTable(tables[0], tables[1], tables[2])

        assertNotNull(rawResult)
    }

    @Test
    fun releaseSold() {
        val doc = Jsoup.parse(File("$testData/release_sold.html"))
        val tables = doc.getElementsByTag("div")[0].children()
        val rawResult = parseReleaseTable(tables[0], tables[1], tables[2])

        assertNotNull(rawResult)
    }
}