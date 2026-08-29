package com.muandrew.forecast.model

import kotlin.math.abs

@kotlin.jvm.JvmInline
value class Money(val value: Long) {

    infix operator fun plus(other: Money): Money = Money(this.value + other.value)
    infix operator fun minus(other: Money): Money = Money(this.value - other.value)
    infix operator fun times(other: Int): Money = Money(this.value * other)
    infix operator fun times(other: Double): Money = Money((this.value * other).toLong())

    infix operator fun div(denominator: Long): DivRes {
        require(denominator > 0) { "denominator must be positive, it is $denominator instead." }
        val res = this.value / denominator
        val rem = this.value % denominator
        return DivRes(Money(res), Money(rem))
    }

    infix operator fun compareTo(other: Money): Int = this.value.compareTo(other.value)

    fun toFormattedString(): String {
        val absVal = abs(value)
        val dollars = absVal / 100
        val cents = absVal % 100
        val centsStr = if (cents < 10) "0$cents" else "$cents"
        return if (value < 0) {
            "($$dollars.$centsStr)"
        } else {
            "$$dollars.$centsStr"
        }
    }

    override fun toString(): String = toFormattedString()

    companion object {
        val ZERO = Money(0)
        val MIN_VALUE = Money(Long.MIN_VALUE)

        fun ofDollars(dollars: Long): Money = Money(dollars * 100)
        fun ofCents(cents: Long): Money = Money(cents)

        fun min(lhs: Money, rhs: Money): Money = Money(kotlin.math.min(lhs.value, rhs.value))
        fun max(lhs: Money, rhs: Money): Money = Money(kotlin.math.max(lhs.value, rhs.value))
    }
}

data class DivRes(val res: Money, val rem: Money)
