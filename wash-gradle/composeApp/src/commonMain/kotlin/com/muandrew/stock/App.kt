package com.muandrew.stock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                LotsUi(w.lots) { TODO() }
            }

            RootState.Transactions -> {
                ReportsUi(w.events) {
//                    state = RootState.SaleReport(it)
                }
            }

            is RootState.SaleReport -> {
                SaleReportUi(currentState.report)
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