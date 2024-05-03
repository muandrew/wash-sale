package com.muandrew.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.components.backstack.BackStack
import com.bumble.appyx.components.backstack.BackStackModel
import com.bumble.appyx.components.backstack.operation.pop
import com.bumble.appyx.components.backstack.operation.push
import com.bumble.appyx.components.backstack.ui.fader.BackStackFader
import com.bumble.appyx.navigation.composable.AppyxNavigationContainer
import com.bumble.appyx.navigation.modality.NodeContext
import com.bumble.appyx.navigation.node.Node
import com.bumble.appyx.navigation.node.node
import com.bumble.appyx.utils.multiplatform.Parcelable
import com.bumble.appyx.utils.multiplatform.Parcelize
import com.bumble.appyx.utils.multiplatform.RawValue
import com.muandrew.stock.model.Lot
import com.muandrew.stock.model.LotReference
import com.muandrew.stock.model.TransactionReference
import com.muandrew.stock.model.TransactionReport
import com.muandrew.stock.world.World
import com.muandrew.stock.world.filterByReference

class RootNode(
    nodeContext: NodeContext,
    public val world: World,
    private val backStack: BackStack<NavTarget> = BackStack(
        model = BackStackModel(
            initialTarget = NavTarget.Dummy,
            savedStateMap = nodeContext.savedStateMap,
        ),
        visualisation = { BackStackFader(it) }
    )
) : Node<RootNode.NavTarget>(
    appyxComponent = backStack,
    nodeContext = nodeContext
) {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
        ) {
            // Let's include the elements of our component into the composition
            AppyxNavigationContainer(
                appyxComponent = backStack,
                modifier = Modifier.weight(0.9f)
            )

            // Let's also add some controls so we can test it
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.1f)
            ) {
                TextButton(onClick = {
                    backStack.push(
                        NavTarget.Reports(
                            world.events,
                            ::onTransactionRefClicked
                        )
                    )
                }) {
                    Text(text = "Push Reports")
                }
                TextButton(onClick = { backStack.push(NavTarget.Lots(world.lots)) }) {
                    Text(text = "Push Lots")
                }
                TextButton(onClick = { backStack.push(NavTarget.MergedLots(world.lots)) }) {
                    Text(text = "Merged Push Lots")
                }
                TextButton(onClick = { backStack.pop() }) {
                    Text(text = "Pop")
                }
            }
        }
    }

    private fun onSaleReportChosen(sale: TransactionReport) {
        backStack.push(NavTarget.TransactionReportNT(listOf(sale)))
    }

    private fun onTransactionRefClicked(ref: TransactionReference) {
        val res = world.events.filter {
            it.ref.date == ref.date &&
                    if (ref.referenceNumber != null) {
                        it.ref.referenceNumber == ref.referenceNumber
                    } else true
        }
        backStack.push(NavTarget.TransactionReportNT(res))
    }

    override fun buildChildNode(
        reference: NavTarget,
        nodeContext: NodeContext
    ) =
        when (reference) {
            NavTarget.Dummy -> node(nodeContext) {
                backStack.push(
                    NavTarget.Reports(
                        world.events,
                        ::onTransactionRefClicked
                    )
                )
            }

            is NavTarget.Reports -> ReportsNode(
                nodeContext,
                reference.reports,
                reference.reportChosen
            )

            is NavTarget.LotNT -> LotNode(
                nodeContext,
                reference.lot,
                ::onTransactionRefClicked,
                ::onLotRefClicked,
            )
            is NavTarget.Lots -> LotsNode(nodeContext, reference.lots, ::onLotChosen)
            is NavTarget.TransactionReportNT -> TransactionReportNode(nodeContext, reference.report)
            is NavTarget.MergedLots -> MergedLotsNode(nodeContext, reference.lots, ::onLotsSelected)
        }

    private fun onLotRefClicked(lotReference: LotReference) {
        val lots = world.lots.filterByReference(lotReference)
        backStack.push(NavTarget.Lots(lots))
    }

    private fun onLotsSelected(lots: List<Lot>) {
        backStack.push(NavTarget.Lots(lots))
    }

    private fun onLotChosen(lot: Lot) {
        backStack.push(NavTarget.LotNT(lot))
    }


    sealed class NavTarget : Parcelable {

        @Parcelize
        data object Dummy : NavTarget()

        @Parcelize
        data class Reports(
            val reports: @RawValue List<TransactionReport>,
            val reportChosen: TransactionRefClicked,
        ) : NavTarget()

        @Parcelize
        data class LotNT(val lot: @RawValue Lot) : NavTarget()

        @Parcelize
        data class Lots(val lots: @RawValue List<Lot>) : NavTarget()

        @Parcelize
        data class MergedLots(val lots: @RawValue List<Lot>) : NavTarget()

        @Parcelize
        data class TransactionReportNT(val report: @RawValue List<TransactionReport>) : NavTarget()
    }
}

typealias TransactionRefClicked = (ref: TransactionReference) -> Unit
typealias LotRefClicked = (ref: LotReference) -> Unit
typealias LotsSelected = (lots: List<Lot>) -> Unit