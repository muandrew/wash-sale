package com.muandrew.stock

object CollectionUtil {
    fun List<Int>.sumReduction(): Int =
        this.reduceOrNull { acc, i -> acc + i } ?: 0


    fun List<Long>.sumReduction(): Long =
        this.reduceOrNull { acc, i -> acc + i } ?: 0L

}