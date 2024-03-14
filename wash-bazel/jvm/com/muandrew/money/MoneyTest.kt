package com.muandrew.money

import org.junit.Assert.assertEquals
import org.junit.Test


@Suppress("FunctionName")
class MoneyTest {

    @Test
    fun addition() {

        val res = Money(1) + Money(2)

        assert(res == Money(3))
    }

    @Test
    fun `division with long - basic`() {

        val res = Money(6) / 2

        assert(res.res == Money(3))
        assert(res.rem == Money.ZERO)
    }

    @Test
    fun `division with long - remainder`() {

        val res = Money(6) / 4

        assert(res.res == Money(1))
        assert(res.rem == Money(2))
    }

    @Test
    fun `division negative Money with long - remainder`() {

        val res = Money(-6) / 4

        assert(res.res == Money(-1))
        assert(res.rem == Money(-2))
    }

    @Test
    fun `drop remainder`() {
        var droppedRemainder: Money = Money.MIN_VALUE

        val res = (Money(6) / 4).dropRemainder {
            droppedRemainder = it
        }

        assert(res == Money(1))
        assert(droppedRemainder == Money(2))
    }

    @Test
    fun `division with Money - basic`() {

        val res = Money(6) / Money(2)

        assert(res.res == Money(3))
        assert(res.rem == Money.ZERO)
    }

    @Test
    fun `division with Money - remainder`() {

        val res = Money(6) / Money(4)

        assert(res.res == Money(1))
        assert(res.rem == Money(2))
    }

    @Test
    fun parseSuccess() {
        val inputs = listOf(
            "$10" to Money(10_00),
            "$10.00" to Money(10_00),
            "$10.01" to Money(10_01),
            "($10.01)" to Money(-10_01),
        )

        for (input in inputs) {
            val res = Money.parse(input.first)

            assertEquals(input.second, res)
        }
    }
}