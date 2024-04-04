package com.muandrew.stock.model

import com.muandrew.money.Money
import kotlin.math.abs
import kotlin.math.min

data class LotValue(
    val shares: Long,
    val value: Money,
) {

    operator fun plus(other: LotValue): LotValue =
        LotValue(this.shares + other.shares, this.value + other.value)

    operator fun minus(other: LotValue): LotValue =
        LotValue(this.shares - other.shares, this.value - other.value)

    companion object {
        val ZERO = LotValue(0, Money.ZERO)
    }
}

fun minShares(lhs: LotValue, rhs: LotValue): Long {
    return min(lhs.shares, rhs.shares)
}

data class SplitResult(
    val split: LotValue,
    val remainder: LotValue = LotValue.ZERO
)

fun LotValue.splitOut(splitShares: Long): SplitResult {
    assert(splitShares <= shares) { "can't split $splitShares share(s) out of a set that only has $shares" }
    if (splitShares == shares) {
        return SplitResult(this.copy())
    }
    val sign = if (value.value < 0L) { -1 } else { 1 }
    val workingValue = abs(value.value)
    val numerator = workingValue * splitShares
    val splitValue = numerator / shares
    val mod = numerator % shares
    val extraBit = if (mod > shares / 2) 1 else 0
    val splitResult = Money(sign * (splitValue + extraBit))
    return SplitResult(
        LotValue(splitShares, splitResult),
        LotValue(shares - splitShares, value - splitResult)
    )
}
