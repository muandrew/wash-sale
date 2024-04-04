package com.muandrew.stock

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumble.appyx.navigation.modality.NodeContext
import com.bumble.appyx.navigation.node.LeafNode
import com.muandrew.stock.model.Lot
import java.time.LocalDate

class MergedLotsNode(
    nodeContext: NodeContext,
    private val lots: List<Lot>,
    private val lotsSelected: LotsSelected,
) : LeafNode(
    nodeContext = nodeContext
) {
    @Composable
    override fun Content(modifier: Modifier) {
        MergedLotsUi(lots, lotsSelected)
    }
}

class MergeData(
    val raw: List<Lot>,
    val date: LocalDate,
    val lot: Int,
    val shares: Long,
    val sharesWashed: Long,
    val washedTransactions: List<Pair<String?, Long>>
)

private data class MDIntermediate(
    val raw: Lot,
    val shares: Long,
    val isReplacement: Boolean,
    val runId: String,
    val creationTransactionNumber: String?
)

@Composable
fun MergedLotsUi(lots: List<Lot>, lotsSelected: LotsSelected) {
    val merged = remember {
        val lotmap = lots
            .groupBy { it.date to it.lot }
            .mapValues { entry ->
                entry.value.map {
                    MDIntermediate(
                        it,
                        it.current.shares,
                        it.isReplacement,
                        it.runId,
                        it.sourceTransaction.referenceNumber
                    )
                }
            }
            .entries
            .map { entry ->
                MergeData(
                    raw = entry.value.map { it.raw },
                    date = entry.key.first,
                    lot = entry.key.second,
                    shares = entry.value.map { it.shares }.reduce { a, b -> a + b },
                    sharesWashed = entry.value
                        .filter { it.isReplacement }
                        .map { it.shares }
                        .reduceOrNull { a, b -> a + b } ?: 0,
                    washedTransactions = entry.value
                        .filter { it.isReplacement }
                        .map { it.creationTransactionNumber to it.shares }
                )
            }
        lotmap.sortedWith(Comparator<MergeData> { a, b ->
            a.date.compareTo(b.date)
        }.thenBy { it.lot })
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("date:lot number, initial shares, shares, shares washed")
        LazyColumn(Modifier.fillMaxWidth()) {
            item {
                Text(
                    fontWeight = FontWeight.Bold,
                    text = "Merged Lot"
                )
            }
            items(merged) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
                    .padding(4.dp)
                    .clickable {
                        lotsSelected(it.raw)
                    }) {
                    val initialShares = it.raw.find { !it.isReplacement }!!.initial.shares
                    Text("${it.date}:${it.lot}, ${initialShares}, ${it.shares}, ${it.sharesWashed};")
                    if (it.washedTransactions.isNotEmpty()) {
                        Text("washed: ${it.washedTransactions.joinToString(",")}")
                    }
                }
            }
        }
    }
}