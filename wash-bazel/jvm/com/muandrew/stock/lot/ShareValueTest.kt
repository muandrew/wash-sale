package com.muandrew.stock.lot

import com.muandrew.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareValueTest {

    @Test
    fun splitOut() {
        val input = ShareValue(10, Money(11))

        val split3Res = input.splitOut(3)

        assertEquals(split3Res.split, ShareValue(3, Money(3)))
        assertEquals(split3Res.remainder, ShareValue(7, Money(8)))
        assertEquals(input, split3Res.split + split3Res.remainder)

        val split10Res = input.splitOut(10)

        assertEquals(split10Res.split, ShareValue(10, Money(11)))
        assertEquals(split10Res.remainder, ShareValue.ZERO)
        assertEquals(input, split10Res.split + split10Res.remainder)
    }
}