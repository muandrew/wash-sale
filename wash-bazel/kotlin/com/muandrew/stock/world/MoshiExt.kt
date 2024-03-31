package com.muandrew.stock.world

import com.muandrew.moshi.adapters.LocalDateAdapter
import com.muandrew.moshi.adapters.LocalTimeAdapter
import com.muandrew.stock.model.LotReference
import com.muandrew.stock.model.TransactionReference
import com.muandrew.stock.model.TransformedFrom
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

object MoshiExt {

    fun Moshi.Builder.addStockAdapters() : Moshi.Builder {
        return this.add(LocalDateAdapter())
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
    }
}