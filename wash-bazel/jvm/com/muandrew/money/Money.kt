package com.muandrew.money

import kotlin.math.abs

@JvmInline
value class Money(val value: Long) {

    infix operator fun plus(other: Money): Money {
        return Money(this.value + other.value)
    }

    infix operator fun minus(other: Money): Money {
        return Money(this.value - other.value)
    }

    infix operator fun times(other: Money): Money {
        return Money(this.value * other.value)
    }

    infix operator fun div(denominator: Long): DivRes {
        assert(denominator > 0) {
            "denominator must be positive, it is $denominator instead."
        }
        val res = this.value / denominator
        val rem = this.value % denominator
        return DivRes(Money(res), Money(rem))
    }

    infix operator fun div(other: Money): DivRes {
        val res = this.value / other.value
        val rem = this.value % other.value
        return DivRes(Money(res), Money(rem))
    }

    infix operator fun compareTo(other: Money): Int {
        return this.value.compareTo(other.value)
    }

    override fun toString(): String {
        val abs = abs(value)
        val dollar = abs / 100
        val cents = abs % 100
        return if( value < 0) {
            "$($dollar.$cents)"
        } else {
            "$$dollar.$cents"
        }
    }

    companion object {
        val ZERO = Money(0)
        val MIN_VALUE = Money(Long.MIN_VALUE)

        fun min(lhs: Money, rhs: Money) : Money {
            return Money(Math.min(lhs.value, rhs.value))
        }

        fun parse(value: String): Money {
            //TODO
            return ZERO
        }
    }
}

infix operator fun Long.times(other: Money): Money {
    return Money(this * other.value)
}

data class DivRes(val res: Money, val rem: Money)

fun DivRes.dropRemainder(run: ((droppedAmount: Money) -> Unit)): Money {
    run(rem)
    return res
}