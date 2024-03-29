package com.muandrew.stock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.muandrew.stock.model.TransactionReport

@Composable
fun App() {
    MaterialTheme {
        val w = remember { Wash.create() }
        var state by remember { mutableStateOf<RootState>(RootState.Home) }
        when (val currentState = state) {
            is RootState.Home -> {
                LazyColumn {
                    items(w.events) { report ->
                        when (report) {
                            is TransactionReport.SaleReport -> {
                                Button(onClick = {
                                    state = RootState.SaleReport(report)
                                }) {
                                    Text("shares involved ${report.shares}")
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
            is RootState.SaleReport -> {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
    data class SaleReport(val report: TransactionReport.SaleReport) : RootState
}