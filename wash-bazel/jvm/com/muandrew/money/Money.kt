package com.muandrew.money

@JvmInline
value class Money(private val value: Long) {

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

    companion object {
        val ZERO = Money(0)
        val MIN_VALUE = Money(Long.MIN_VALUE)
    }
}

data class DivRes(val res: Money, val rem: Money)

fun DivRes.dropRemainder(run: ((droppedAmount: Money) -> Unit)): Money {
    run(rem)
    return res
}