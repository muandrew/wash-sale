package com.muandrew.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumble.appyx.navigation.modality.NodeContext
import com.bumble.appyx.navigation.node.LeafNode
import com.muandrew.stock.model.TransactionReport

class ReportsNode(
    nodeContext: NodeContext,
    private val reports: List<TransactionReport>,
    private val reportChosen: TransactionRefClicked,
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
    reportChosen: TransactionRefClicked
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                fontWeight = FontWeight.Bold,
                text = "Reports"
            )
        }
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
                            .clickable {
                                reportChosen(report.ref)
                            }
                            .padding(
                                PaddingValues(4.dp)
                            )
                    ) {
                        Text("Sale Reference Number: ${report.ref.referenceNumber}")
                        Text("${report.ref.date}")
                        Text(
                            "total allowed shares: ${
                                report.allowedTransfer.map { it.shares }
                                    .reduceOrNull { a, b -> a + b } ?: 0
                            }")
                        Text(
                            "total disallowed shares: ${
                                report.disallowedTransfer.map { it.shares }
                                    .reduceOrNull { a, b -> a + b } ?: 0
                            }")
                    }

                }

                is TransactionReport.ReceivedReport -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(2.dp, Color.Black),
                                RoundedCornerShape(15.dp)
                            )
                            .clickable {
                                reportChosen(report.ref)
                            }
                            .padding(
                                PaddingValues(4.dp)
                            )
                    ) {
                        Text("received basis ${report.costBasis}")
                    }
                }

                else -> Text("meep")
            }
        }
    }
}

