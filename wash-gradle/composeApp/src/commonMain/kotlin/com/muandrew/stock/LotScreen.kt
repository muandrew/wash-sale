package com.muandrew.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bumble.appyx.navigation.modality.NodeContext
import com.bumble.appyx.navigation.node.LeafNode
import com.muandrew.stock.model.Lot


class LotNode(
    nodeContext: NodeContext,
    private val lot: Lot,
    private val trefClicked: TransactionRefClicked,
    private val lotRefClicked: LotRefClicked,
) : LeafNode(
    nodeContext = nodeContext
) {
    @Composable
    override fun Content(modifier: Modifier) {
        LotUi(lot, trefClicked, lotRefClicked)
    }
}

@Composable
fun LotUi(
    lot: Lot,
    trefClicked: TransactionRefClicked,
    lotRefClicked: LotRefClicked
) {
    Column {
        Text(
            fontWeight = FontWeight.Bold,
            text = "Lot"
        )
        Text("runId: ${lot.runId}")
        Text(
            modifier = Modifier.clickable {
                lotRefClicked(lot.ref)
            },
            text = "lot family: ${lot.ref}"
        )
        Text("date: ${lot.date}")
        Text("ini_shaers: ${lot.initial.shares}")
        Text("shares: ${lot.current.shares}")
        Text("value: ${lot.current.value}")
        Text("isReplacement: ${lot.isReplacement}")
        val lotRef = lot.sourceLot
        if (lotRef != null) {
            Text(
                modifier = Modifier.clickable {
                    lotRefClicked(lotRef)
                },
                text = "from lot family: ${lot.sourceLot}"
            )
        }
        Text(
            modifier = Modifier.clickable {
                trefClicked(lot.sourceTransaction)
            },
            text = "frm transaction: ${lot.sourceTransaction.referenceNumber}"
        )
        Text("\n")

        lot.wireTransactions.forEach { change ->
            Text(
                modifier = Modifier.clickable { trefClicked(change.transactionReference) },
                text = "${change.transactionReference.referenceNumber} shares: ${change.change.shares} value: ${change.change.value}"
            )
        }
    }
}
