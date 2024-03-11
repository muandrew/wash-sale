package com.muandrew.stock.lot

import com.muandrew.money.Money
import com.muandrew.money.times

data class ShareValue(
    val shares: Long,
    val value: Money,
) {
    companion object {
        val ZERO = ShareValue(0, Money.ZERO)
    }
}

data class ApplicationResult(
    val targetRemaining: ShareValue,
    val accumulatedChanges: ShareValue,
)

fun <T : Any> applySharesAmongCandidates(
    source: ShareValue,
    candidates: List<T>,
    getLotSnapshotFromCandidate: T.() -> ShareValue,
    updateCandidate: T.(targetToApply: ShareValue) -> ShareValue,
): ApplicationResult {
    val valuePerShareDev = source.value / source.shares
    val valuePerShare = valuePerShareDev.res

    var remainingShares = source.shares
    var accumulateOtherShares = 0L
    var accumulateOtherValue = Money.ZERO
    for (other in candidates) {
        val otherSnapshot = other.getLotSnapshotFromCandidate()
        val targetToApply = if (otherSnapshot.shares >= remainingShares) {
            ShareValue(remainingShares, remainingShares * valuePerShare + valuePerShareDev.rem)
        } else {
            ShareValue(otherSnapshot.shares, otherSnapshot.shares * valuePerShare)
        }
        val toAccumulate = other.updateCandidate(targetToApply)
        accumulateOtherShares += toAccumulate.shares
        accumulateOtherValue += toAccumulate.value
        remainingShares -= toAccumulate.shares
    }
    return ApplicationResult(
        ShareValue(
            remainingShares, if (remainingShares == 0L) {
                Money.ZERO
            } else {
                remainingShares * valuePerShare + valuePerShareDev.rem
            }
        ),
        ShareValue(
            accumulateOtherShares, accumulateOtherValue
        )
    )
}

data class SplitResult(
    val split: ShareValue,
    val remainder: ShareValue = ShareValue.ZERO
)

fun ShareValue.splitOut(splitShares: Long): SplitResult {
    assert(splitShares <= shares) { "can't split $splitShares share(s) out of a set that only has $shares" }
    if (splitShares == shares) {
        return SplitResult(this.copy())
    }
    val perShare = value / shares
    val splitValue = splitShares * perShare.res
    return SplitResult(
        ShareValue(splitShares, splitValue),
        ShareValue(shares - splitShares, value - splitValue)
    )
}

operator fun ShareValue.plus(other: ShareValue): ShareValue =
    ShareValue(this.shares + other.shares, this.value + other.value)
