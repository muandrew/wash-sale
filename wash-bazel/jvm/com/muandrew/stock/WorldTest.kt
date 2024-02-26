package com.muandrew.stock

import com.muandrew.money.Money
import org.junit.Test
import java.time.LocalDate

class WorldTest {

    @Test
    fun releaseTest() {
        val w = World()
        w.release(
            "2000-01-01",
            10,
            10_000
        )
        assert(w.lots.size == 1)
        assert(w.lots[0].current.shares == 10L)
        assert(w.lots[0].current.costBasis == Money(10_000))
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
        assert(w.lots.size == 1)
        assert(w.lots[0].current.shares == 9L)
        assert(w.lots[0].current.costBasis == Money(9_000))
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