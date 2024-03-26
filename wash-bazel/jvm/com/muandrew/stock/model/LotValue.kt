package com.muandrew.stock.model

import com.muandrew.money.Money
import com.muandrew.money.times
import kotlin.math.abs
import kotlin.math.min

data class LotValue(
    val shares: Long,
    val value: Money,
) {
    companion object {
        val ZERO = LotValue(0, Money.ZERO)
    }
}

data class ApplicationResult(
    val targetRemaining: LotValue,
    val accumulatedChanges: LotValue,
)

fun minShares(lhs: LotValue, rhs: LotValue): Long {
    return min(lhs.shares, rhs.shares)
}

fun <T : Any> applySharesAmongCandidates(
    source: LotValue,
    candidates: List<T>,
    getLotSnapshotFromCandidate: T.() -> LotValue,
    updateCandidate: T.(targetToApply: LotValue) -> LotValue,
): ApplicationResult {
    val valuePerShareDev = source.value / source.shares
    val valuePerShare = valuePerShareDev.res

    var remainingShares = source.shares
    var accumulateOtherShares = 0L
    var accumulateOtherValue = Money.ZERO
    for (other in candidates) {
        val otherSnapshot = other.getLotSnapshotFromCandidate()
        val targetToApply = if (otherSnapshot.shares >= remainingShares) {
            LotValue(remainingShares, remainingShares * valuePerShare + valuePerShareDev.rem)
        } else {
            LotValue(otherSnapshot.shares, otherSnapshot.shares * valuePerShare)
        }
        val toAccumulate = other.updateCandidate(targetToApply)
        accumulateOtherShares += toAccumulate.shares
        accumulateOtherValue += toAccumulate.value
        remainingShares -= toAccumulate.shares
    }
    return ApplicationResult(
        LotValue(
            remainingShares, if (remainingShares == 0L) {
                Money.ZERO
            } else {
                remainingShares * valuePerShare + valuePerShareDev.rem
            }
        ),
        LotValue(
            accumulateOtherShares, accumulateOtherValue
        )
    )
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
    val perShare = workingValue / shares
    val remainder = workingValue % shares
    val splitValue = splitShares * perShare + min(splitShares, remainder)
    val splitResult = Money(sign * splitValue)
    return SplitResult(
        LotValue(splitShares, splitResult),
        LotValue(shares - splitShares, value - splitResult)
    )
}

operator fun LotValue.plus(other: LotValue): LotValue =
    LotValue(this.shares + other.shares, this.value + other.value)
