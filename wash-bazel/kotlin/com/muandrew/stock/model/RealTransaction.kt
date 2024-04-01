package com.muandrew.stock.model

import com.muandrew.money.Money
import java.time.LocalDate

interface RealTransaction {

    val referenceNumber: String
    val date: LocalDate
    val grantDate: LocalDate

    data class ReleaseWithheld(
        override val referenceNumber: String,
        override val date: LocalDate,
        override val grantDate: LocalDate,
        val gross: LotValue,
        val disbursed: LotValue,
        val withheld: LotValue = LotValue.ZERO,
        val releasePrice: Money,
    ) : RealTransaction {

        // basic check to make sure math is working
        init {
            assert(withheld != LotValue.ZERO)
            assert(gross == disbursed + withheld) {
                "gross should be equal to disbursed + withheld\n$this"
            }
        }
    }

    data class ReleaseSold(
        override val referenceNumber: String,
        override val date: LocalDate,
        override val grantDate: LocalDate,
        val gross: LotValue,
        val disbursed: LotValue,
        val sold: LotValue = LotValue.ZERO,
        val releasePrice: Money,
        val salePrice: Money? = null,
    ) : RealTransaction {

        // basic check to make sure math is working
        init {
            // value isn't equal because disbursed is what is left over after sale.
            assert(gross.shares == disbursed.shares + sold.shares)
            assert(sold != LotValue.ZERO)
        }
    }
}