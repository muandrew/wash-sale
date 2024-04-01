package com.muandrew.moshi.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.time.LocalTime

class LocalTimeAdapter : JsonAdapter<LocalTime>() {

    @FromJson
    override fun fromJson(reader: JsonReader): LocalTime? {
        return if (reader.peek() != JsonReader.Token.NULL) {
            LocalTime.parse(reader.nextString())
        } else {
            reader.nextNull<Any>()
            null
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: LocalTime?) {
        writer.value(value?.toString())
    }
}