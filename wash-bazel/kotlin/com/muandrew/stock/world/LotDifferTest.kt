package com.muandrew.stock.world

import com.muandrew.testtool.TestFiles
import org.junit.Before
import org.junit.Test

class LotDifferTest {
    lateinit var testData: String

    @Before
    fun setUp() {
        testData = TestFiles.testDirectoryPath("kotlin/com/muandrew/stock/world/testdata/lot")
    }

    @Test
    fun a() {
        val a = LotDiffer.diff("$testData/1_ini.csv", "$testData/1_fin.csv")
        a
    }

}