package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import javafx.scene.paint.Color
import org.pdi.core.AppState
import org.pdi.core.StateContext
import kotlin.math.roundToInt

// tonal curve panel. Very simple
class TonalCurvePanelController {

    @FXML
    private lateinit var tonalCurveCanvas: Canvas

    private lateinit var appState: AppState
    private var selectedChannel: Char = 'L' // Default to Luminosity

    @FXML
    fun initialize(_appState: AppState) {
        appState = _appState
        val toggleGroup = (tonalCurveCanvas.scene?.lookup("#luminosityRadioButton") as? RadioButton)?.toggleGroup
            ?: (tonalCurveCanvas.scene?.lookup("#redRadioButton") as? RadioButton)?.toggleGroup

        toggleGroup?.selectedToggleProperty()?.addListener { _, _, newToggle ->
            selectedChannel = (newToggle as RadioButton).userData as Char
            drawTonalCurve()
        }

        appState.addContextListener {
            drawTonalCurve()
        }
        drawTonalCurve()
    }

    private fun drawTonalCurve() {
        val curveLuts = appState.getTonalCurve()
        val gc: GraphicsContext = tonalCurveCanvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, tonalCurveCanvas.width, tonalCurveCanvas.height)

        val width = tonalCurveCanvas.width
        val height = tonalCurveCanvas.height

        // Draw border
        gc.stroke = Color.BLACK
        gc.strokeRect(0.0, 0.0, width, height)

        // Draw diagonal line (identity)
        gc.stroke = Color.LIGHTGRAY
        gc.strokeLine(0.0, height, width, 0.0)

        val lut = curveLuts?.get(selectedChannel)

        if (lut == null || lut.isEmpty()) {
            gc.fill = Color.LIGHTGRAY
            gc.font = javafx.scene.text.Font.font("SansSerif", 14.0)
            gc.fillText("Seleccione una curva o cargue una imagen", 50.0, height / 2)
            return
        }

        val scaleX = width / 255.0
        val scaleY = height / 255.0

        gc.stroke = when (selectedChannel) {
            'R' -> Color.RED
            'G' -> Color.GREEN
            'B' -> Color.BLUE
            else -> Color.GRAY // Luminosity
        }
        gc.lineWidth = 2.0

        gc.beginPath()
        gc.moveTo(0.0, height - lut[0] * scaleY)

        for (i in 1..255) {
            val x = i * scaleX
            val y = height - lut[i] * scaleY
            gc.lineTo(x, y)
        }
        gc.stroke()
    }
}
