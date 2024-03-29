package com.muandrew.stock

import com.muandrew.money.Money
import com.muandrew.stock.model.Transaction
import com.muandrew.stock.world.World
import java.time.LocalDate

object Wash {
    fun create(): World {
        val w = World()
        w.acceptTransaction(
            Transaction.createRelease(
                date = LocalDate.parse("2024-01-01"),
                shares = 10,
                value = Money(1000)
            )
        )
        w.acceptTransaction(
            Transaction.createSale(
                date = LocalDate.parse("2024-01-02"),
                shares = 1,
                value = Money(10),
                lotDate = LocalDate.parse("2024-01-01"),
            )
        )
        w.acceptTransaction(
            Transaction.createSale(
                date = LocalDate.parse("2024-01-02"),
                shares = 1,
                value = Money(1),
                lotDate = LocalDate.parse("2024-01-01"),
            )
        )
        return w
    }
}