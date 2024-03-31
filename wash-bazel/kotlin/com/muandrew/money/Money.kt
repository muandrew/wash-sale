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
        return if (value < 0) {
            "($$dollar.%02d)".format(cents)
        } else {
            "$$dollar.%02d".format(cents)
        }
    }

    companion object {
        val ZERO = Money(0)
        val MIN_VALUE = Money(Long.MIN_VALUE)
        private val regex = Regex("""^\w*(\()?\$(\d+)(\.(\d{2}))?(\))?\w*$""")

        fun min(lhs: Money, rhs: Money): Money {
            return Money(kotlin.math.min(lhs.value, rhs.value))
        }

        fun parse(value: String): Money {
            val res = regex.find(value) ?: throw IllegalArgumentException("could not parse $value as Money")
            val (negStart, dollars, _ /*dot*/, strCents, negEnd) = res.destructured
            if (negStart.isNotEmpty() xor negEnd.isNotEmpty()) {
                throw IllegalArgumentException("you need to start and end parenthesis")
            }
            val sign = if (negStart.isNotEmpty() && negEnd.isNotEmpty()) {
                -1
            } else {
                1
            }
            val cents = if (strCents.isNotEmpty()) {
                strCents.toLong()
            } else {
                0
            }
            return Money(sign * (dollars.toLong() * 100 + cents))
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