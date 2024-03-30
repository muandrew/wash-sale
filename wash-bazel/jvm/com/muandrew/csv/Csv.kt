package com.muandrew.csv

import com.github.doyaaaaaken.kotlincsv.dsl.context.CsvReaderContext
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File

object Csv {
    fun read(csvFilePath: String, init: CsvReaderContext.() -> Unit = {}): List<Map<String, String>> {
        return csvReader(init = init).readAllWithHeader(File(csvFilePath))
    }
}