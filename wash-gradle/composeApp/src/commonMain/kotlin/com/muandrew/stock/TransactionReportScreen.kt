package com.muandrew.stock

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumble.appyx.navigation.modality.NodeContext
import com.bumble.appyx.navigation.node.LeafNode
import com.muandrew.stock.model.TransactionReport

class TransactionReportNode(
    nodeContext: NodeContext,
    private val report: List<TransactionReport>
) : LeafNode(
    nodeContext = nodeContext
) {
    @Composable
    override fun Content(modifier: Modifier) {
        Column {
            Text(
                fontWeight = FontWeight.Bold,
                text = "Transaction Report"
            )
            report.forEach {
                when (it) {
                    is TransactionReport.ReceivedReport -> ReceivedReportUi(it)
                    is TransactionReport.SaleReport -> SaleReportUi(it)
                }
            }
        }
    }
}

@Composable
fun ReceivedReportUi(report: TransactionReport.ReceivedReport) {
    Column(
        Modifier.fillMaxWidth().border(2.dp, Color.Green).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("received: ${report.ref.date}: ${report.ref.referenceNumber}")
        Text("shares: ${report.shares}, ${report.costBasis}")
    }
}

@Composable
fun SaleReportUi(report: TransactionReport.SaleReport) {
    Column(
        Modifier.fillMaxWidth().border(2.dp, Color.Red).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("date: ${report.ref.date}")
        Text("refNumber: ${report.ref.referenceNumber}")
        Text("shares: ${report.shares}")
        LazyColumn {
            item { Text("allowed:") }
            items(report.allowedTransfer) {
                Column(modifier = Modifier.border(1.dp, Color.Black)) {
                    Text("shares: ${it.shares}, basis: ${it.basis}, soldLotId: ${it.soldLotId}")
                }
            }
            item { Text("disallowed:") }
            items(report.disallowedTransfer) {
                Column(modifier = Modifier.border(1.dp, Color.Black)) {
                    Text("shares: ${it.shares}, basis: ${it.basis}, soldLotId: ${it.soldLotId}")
                    Text("disallowedValue: ${it.disallowedValue}, toLot: ${it.transferredLotId}, res: ${it.resultingId}")
                }
            }
        }
    }
}