package com.muandrew.stock.model

import com.muandrew.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class LotValueTest {

    data class SplitOutCase(
        val shares: Long,
        val value: Long,
        val splitShares: Long,
        val splitValue: Long,
        val remShares: Long,
        val remValue: Long,
    )

    @Test
    fun splitOut() {
        val cases = listOf(
            SplitOutCase(10, 11, 3, 3, 7, 8),
            SplitOutCase(10, 11, 10, 11, 0, 0),
            SplitOutCase(100, 199, 3, 3, 97, 196),
        )
        for (case in cases) {
            val input = LotValue(case.shares, Money(case.value))

            val res = input.splitOut(case.splitShares)

            assertEquals(case.splitShares, res.split.shares)
            assertEquals(Money(case.splitValue), res.split.value)
            assertEquals(case.remShares, res.remainder.shares)
            assertEquals(Money(case.remValue), res.remainder.value)

            assertEquals(input, res.split + res.remainder)
        }
    }
}