package com.muandrew.stock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bumble.appyx.navigation.modality.NodeContext
import com.bumble.appyx.navigation.node.LeafNode
import com.muandrew.stock.model.Lot
import java.time.LocalDate

class MergedLotsNode(
    nodeContext: NodeContext,
    private val lots: List<Lot>
) : LeafNode(
    nodeContext = nodeContext
) {
    @Composable
    override fun Content(modifier: Modifier) {
        MergedLotsUi(lots)
    }
}

class MergeData(
    val date: LocalDate,
    val lot: Int,
    val shares: Long,
    val sharesWashed: Long,
    val washedTransactions: List<Pair<String?, Long>>
)

private data class MDIntermediate(
    val shares: Long,
    val isReplacement: Boolean,
    val runId: String,
    val creationTransactionNumber: String?
)

@Composable
fun MergedLotsUi(lots: List<Lot>) {
    val merged = remember {
        val lotmap = lots
            .groupBy { it.date to it.lot }
            .mapValues { entry ->
                entry.value.map {
                    MDIntermediate(
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
    SelectionContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("date\tlot number\tshares\tshares washed")
            LazyColumn(Modifier.fillMaxWidth()) {
                items(merged) {
                    Text("${it.date}\t${it.lot}\t${it.shares}\t${it.sharesWashed};")
                    if (it.washedTransactions.isNotEmpty()) {
                        Text("washed: ${it.washedTransactions.joinToString(",")}")
                    }
                }
            }
        }
    }
}