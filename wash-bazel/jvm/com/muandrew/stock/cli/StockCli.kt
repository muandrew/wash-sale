package com.muandrew.stock.cli

import com.muandrew.money.Money
import com.muandrew.moshi.adapters.LocalDateAdapter
import com.muandrew.moshi.adapters.LocalTimeAdapter
import com.muandrew.stock.*
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
                PolymorphicJsonAdapterFactory.of(LotIdentifier::class.java, "type")
                    .withSubtype(LotIdentifier.DateLotIdentifier::class.java, "date")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(TransactionId::class.java, "type")
                    .withSubtype(TransactionId.DateId::class.java, "date")
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
}