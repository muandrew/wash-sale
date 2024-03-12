package com.muandrew.stock

import com.muandrew.moshi.adapters.LocalDateAdapter
import com.muandrew.moshi.adapters.LocalTimeAdapter
import com.muandrew.stock.model.LotIdentifier
import com.muandrew.stock.model.TransactionId
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
                PolymorphicJsonAdapterFactory.of(LotIdentifier::class.java, "type")
                    .withSubtype(LotIdentifier.DateLotIdentifier::class.java, "date")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(TransactionId::class.java, "type")
                    .withSubtype(TransactionId.DateId::class.java, "date")
            )
    }
}