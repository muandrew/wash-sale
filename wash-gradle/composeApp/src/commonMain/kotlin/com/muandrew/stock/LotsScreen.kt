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
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bumble.appyx.navigation.modality.NodeContext
import com.bumble.appyx.navigation.node.LeafNode
import com.muandrew.stock.model.Lot
import java.time.LocalDate

class LotsNode(
    nodeContext: NodeContext,
    private val lots: List<Lot>
) : LeafNode(
    nodeContext = nodeContext
) {
    @Composable
    override fun Content(modifier: Modifier) {
        LotsUi(lots)
    }
}

@Composable
fun LotsUi(lots: List<Lot>) {
    Column {
        var textField by remember { mutableStateOf("") }
        var displayedLots by remember { mutableStateOf(lots) }
        TextField(
            value = textField,
            onValueChange = {
                textField = it
                if (it.isEmpty()) {
                    displayedLots = lots
                } else {
                    try {
                        val date = LocalDate.parse(it)
                        displayedLots = lots.filter { lot: Lot -> lot.date == date }
                    } catch (_: Exception) {
                    }
                }
            })
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayedLots) {
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
