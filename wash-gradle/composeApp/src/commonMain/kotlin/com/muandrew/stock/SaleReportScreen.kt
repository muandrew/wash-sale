package com.muandrew.stock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bumble.appyx.navigation.modality.NodeContext
import com.bumble.appyx.navigation.node.LeafNode
import com.muandrew.stock.model.TransactionReport

class SaleReportNode(
    nodeContext: NodeContext,
    private val report: TransactionReport.SaleReport
) : LeafNode(
    nodeContext = nodeContext
) {
    @Composable
    override fun Content(modifier: Modifier) {
        SaleReportUi(report)
    }
}

@Composable
fun SaleReportUi(report : TransactionReport.SaleReport) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("date: ${report.date}")
        Text("shares: ${report.shares}")
        LazyColumn {
            item { Text("allowed:") }
            items(report.allowedTransfer) {
                Text("shares: ${it.shares}, basis: ${it.basis}, ${it.soldLotId}")
            }
            item { Text("disallowed:") }
            items(report.disallowedTransfer) {
                Text("shares: ${it.shares}, basis: ${it.basis}, ${it.soldLotId}")
                Text("disallowedValue: ${it.disallowedValue}, frm: ${it.transferredLotId}, res: ${it.resultingId}")
            }
        }
    }
}