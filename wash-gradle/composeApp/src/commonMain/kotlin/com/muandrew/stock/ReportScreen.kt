package com.muandrew.stock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
    }
}