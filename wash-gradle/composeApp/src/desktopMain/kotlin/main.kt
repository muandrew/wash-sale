import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.bumble.appyx.navigation.integration.DesktopNodeHost
import com.muandrew.stock.App
import com.muandrew.stock.RootNode
import com.muandrew.stock.Wash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.lang.System.getenv

sealed class Events {
    data object OnBackPressed : Events()
}

fun main() = application {
    val events: Channel<Events> = Channel()
    val windowState = rememberWindowState(size = DpSize(480.dp, 640.dp))
    val eventScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    Window(
        title = "Wash Sale",
        state = windowState,
        onCloseRequest = ::exitApplication,
        onKeyEvent = {
            // See back handling section in the docs below!
            onKeyEvent(it, events, eventScope)
        },
    ) {
            Surface {
                DesktopNodeHost(
                    windowState = windowState,
                    onBackPressedEvents = events.receiveAsFlow().mapNotNull {
                        if (it is Events.OnBackPressed) Unit else null
                    }
                ) {
                    RootNode(
                        nodeContext = it,
                        world = Wash.create(getenv("WASH_DIR")),
                    )
                }
        }
    }
}

private fun onKeyEvent(
    keyEvent: KeyEvent,
    events: Channel<Events>,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
): Boolean =
    when {
        keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace -> {
            coroutineScope.launch { events.send(Events.OnBackPressed) }
            true
        }

        else -> false
    }

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}