package com.muandrew.stock.world

import com.muandrew.moshi.adapters.LocalDateAdapter
import com.muandrew.moshi.adapters.LocalTimeAdapter
import com.muandrew.stock.model.LotReference
import com.muandrew.stock.model.Transaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

object MoshiExt {

    fun Moshi.Builder.addStockAdapters() : Moshi.Builder {
        return this.add(LocalDateAdapter())
            .add(LocalTimeAdapter())
            .add(
                PolymorphicJsonAdapterFactory.of(Transaction::class.java, "type")
                    .withSubtype(Transaction.ReleaseTransaction::class.java, "release")
                    .withSubtype(Transaction.SaleTransaction::class.java, "sale")
            )
    }
}