import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.muandrew.forecast.ui.ForecastApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(
        title = "Financial Forecast",
        canvasElementId = "ComposeTarget"
    ) {
        ForecastApp()
    }
}
