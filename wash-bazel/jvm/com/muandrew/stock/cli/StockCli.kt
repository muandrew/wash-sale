package com.muandrew.stock.cli

import com.muandrew.money.Money
import com.muandrew.stock.Transaction
import com.muandrew.stock.World
import java.time.LocalDate

object StockCli {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Hello World")
        val w = World()

        w.acceptTransaction(Transaction.createRelease(
            date = LocalDate.parse("2024-01-01"),
            shares = 10,
            value = Money(1000))
        )

        println(w)
    }
}