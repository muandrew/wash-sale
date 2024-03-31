package com.muandrew.stock.cli

import com.muandrew.money.Money
import com.muandrew.moshi.adapters.LocalDateAdapter
import com.muandrew.moshi.adapters.LocalTimeAdapter
import com.muandrew.stock.model.LotReference
import com.muandrew.stock.model.Transaction
import com.muandrew.stock.model.TransactionReference
import com.muandrew.stock.model.TransformedFrom
import com.muandrew.stock.world.StockTransactionReader
import com.muandrew.stock.world.World
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.LocalDate

object StockCli {

    @JvmStatic
    fun main(args: Array<String>) {
        val moshi = Moshi.Builder()
            .add(LocalDateAdapter())
            .add(LocalTimeAdapter())
            .add(
                PolymorphicJsonAdapterFactory.of(TransformedFrom::class.java, "type")
                    .withSubtype(TransformedFrom.WashSale::class.java, "wash_sale")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(LotReference::class.java, "type")
                    .withSubtype(LotReference.DateLotReference::class.java, "date")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(TransactionReference::class.java, "type")
                    .withSubtype(TransactionReference.DateReference::class.java, "date")
            )
            .addLast(KotlinJsonAdapterFactory())
            .build()

        println("Hello World")
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

        w.events.forEach {
            it.print()
        }

        println(w)
    }

    fun read(i: String): World {
        val ts = StockTransactionReader.readTransactions(i)
        val w = World()

        val ss = ts.filterIsInstance<Transaction.SaleTransaction>().sortedBy { it.date.date }
        val rs = ts.filterIsInstance<Transaction.ReleaseTransaction>().sortedBy { it.date.date }

        for (r in rs) {
            w.acceptTransaction(r)
        }
        ss.forEach {
            w.acceptTransaction(it)
        }
        return w
    }
}