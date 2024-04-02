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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muandrew.stock.model.TransactionReport

@Composable
fun App() {
    MaterialTheme {
        val w = remember { Wash.create() }
        var state by remember { mutableStateOf<RootState>(RootState.Home) }
        when (val currentState = state) {
            is RootState.Home -> {
                Column(Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        state = RootState.Transactions
                    }) {
                        Text("Transactions")
                    }
                    Button(onClick = {
                        state = RootState.Lots
                    }) {
                        Text("Lots")
                    }
                }
            }

            RootState.Lots -> {
                Column {
                    Button(onClick = { state = RootState.Home }) {
                        Text("Home")
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(w.lots) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        BorderStroke(2.dp, Color.Black),
                                        RoundedCornerShape(15.dp)
                                    )
                                    .padding(
                                        PaddingValues(4.dp)
                                    )
                            ) {
                                Text("runId: ${it.runId}")
                                Text("date: ${it.date}")
                                Text("oshaers: ${it.initial.shares}")
                                Text("shares: ${it.current.shares}")
                                Text("value: ${it.current.value}")
                                Text("isReplacement: ${it.isReplacement}")
                            }
                        }
                    }
                }
            }

            RootState.Transactions -> {
                Column {
                    Button(onClick = { state = RootState.Home }) {
                        Text("Home")
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(w.events) { report ->
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
                                        Button(onClick = {
                                            state = RootState.SaleReport(report)
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
            }

            is RootState.SaleReport -> {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        state = RootState.Home
                    }) {
                        Text("Home")
                    }
                    Text("date: ${currentState.report.date}")
                    Text("shares: ${currentState.report.shares}")
                }
            }

        }
    }
}

sealed interface RootState {
    data object Home : RootState
    data object Transactions : RootState
    data object Lots : RootState
    data class SaleReport(val report: TransactionReport.SaleReport) : RootState
}