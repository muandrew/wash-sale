package com.muandrew.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bumble.appyx.navigation.modality.NodeContext
import com.bumble.appyx.navigation.node.LeafNode
import com.muandrew.stock.model.TransactionReport

class ReportsNode(
    nodeContext: NodeContext,
    private val reports: List<TransactionReport>,
    private val reportChosen: ((TransactionReport.SaleReport) -> Unit)?
) : LeafNode(
    nodeContext = nodeContext
) {
    @Composable
    override fun Content(modifier: Modifier) {
        ReportsUi(reports, reportChosen)
    }
}

@Composable
fun ReportsUi(
    sale: List<TransactionReport>,
    reportChosen: ((TransactionReport.SaleReport) -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sale) { report ->
            when (report) {
                is TransactionReport.SaleReport -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(2.dp, Color.Black),
                                RoundedCornerShape(15.dp)
                            )
                            .padding(
                                PaddingValues(4.dp)
                            )
                    ) {
                        Text("Reference Number: ${report.referenceNumber}")
                        Text("${report.date}")
                        Text(
                            "${
                                report.disallowedTransfer.map { it.shares }
                                    .reduceOrNull { a, b -> a + b } ?: 0
                            }")
                        Button(onClick = {
                            reportChosen?.invoke(report)
                        }) {
                            Text("shares involved ${report.shares}")
                        }
                    }

                }

                is TransactionReport.ReceivedReport -> {
                    Text("received basis ${report.costBasis}")
                }

                else -> Text("meep")
            }
        }
    }
}

