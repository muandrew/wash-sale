package com.muandrew.stock

import androidx.compose.foundation.layout.Column
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

@Composable
fun MergedLotsUi(lots: List<Lot>) {
    val merged = remember {
        val lotmap = lots
            .groupBy { it.date to it.lot }
            .mapValues { entry ->
                entry.value
                    .map { it.current.shares }
                    .reduce { a, b -> a + b }
            }
        lotmap.entries.sortedWith(Comparator<Map.Entry<Pair<LocalDate, Int>, Long>> { a, b ->
            a.key.first.compareTo(b.key.first)
        }.thenBy { it.key.second })
    }
    SelectionContainer {
        Column {
            Text("date\tlot number\tshares")
            LazyColumn {
                items(merged) {
                    Text("${it.key.first}\t${it.key.second}\t${it.value};")
                }
            }
        }
    }
}