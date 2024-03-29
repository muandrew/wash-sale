import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.muandrew.stock.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "com.muandrew.stock.Wash Sale") {
        App()
    }
}

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}