package org.pdi.ui.panels

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ComboBox
import javafx.scene.paint.Color
import org.pdi.core.AppState
import org.pdi.core.luminosity

// histogram panel with selectors for the channels
class HistogramPanelController {

    @FXML
    private lateinit var histogramCanvas: Canvas

    @FXML
    private lateinit var channelComboBox: ComboBox<String>

    private lateinit var appState: AppState
    private var selectedChannel: String = "Red" // Default selected channel

    fun setAppState(appState: AppState) {
        this.appState = appState
        channelComboBox.items.addAll(FXCollections.observableArrayList("Red", "Green", "Blue", "Gray"))
        channelComboBox.selectionModel.selectFirst()

        channelComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                selectedChannel = newValue
                drawHistogram()
            }
        }
        drawHistogram()
    }

    private fun drawHistogram() {
        val histogramData = appState.getHistogram()
        // canvas and graphics 2d content
        val gc: GraphicsContext = histogramCanvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, histogramCanvas.width, histogramCanvas.height)

        if (histogramData == null || histogramData.isEmpty()) {
            return
        }

        val width = histogramCanvas.width
        val height = histogramCanvas.height
        val barWidth = width / 256.0

        val dataToDraw: IntArray
        val colorToDraw: Color

        when (selectedChannel) {
            "Red" -> {
                dataToDraw = histogramData[0] ?: IntArray(256)
                colorToDraw = Color.RED
            }
            "Green" -> {
                dataToDraw = histogramData[1] ?: IntArray(256)
                colorToDraw = Color.GREEN
            }
            "Blue" -> {
                dataToDraw = histogramData[2] ?: IntArray(256)
                colorToDraw = Color.BLUE
            }
            "Gray" -> {
                val redData = histogramData[0] ?: IntArray(256)
                val greenData = histogramData[1] ?: IntArray(256)
                val blueData = histogramData[2] ?: IntArray(256)
                dataToDraw = IntArray(256) { i -> luminosity(java.awt.Color(redData[i],greenData[i],blueData[i])) }
                colorToDraw = Color.GRAY
            }
            else -> {
                dataToDraw = IntArray(256)
                colorToDraw = Color.BLACK
            }
        }

        val maxCount = dataToDraw.maxOrNull() ?: 0

        if (maxCount == 0) {
            return
        }

        gc.fill = colorToDraw
        for (i in 0 until 256) {
            val barHeight = (dataToDraw[i].toDouble() / maxCount * height)
            gc.fillRect(i * barWidth, height - barHeight, barWidth.coerceAtLeast(1.0), barHeight)
        }
    }
}
