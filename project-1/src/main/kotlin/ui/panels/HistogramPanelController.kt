package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import org.pdi.core.AppState
import org.pdi.core.Histogram

class HistogramPanelController {

    @FXML
    private lateinit var histogramCanvas: Canvas

    private lateinit var appState: AppState
    private var histogramData: Histogram? = null

    fun setAppState(appState: AppState) {
        this.appState = appState
        appState.addContextListener {
            histogramData = appState.getHistogram()
            drawHistogram()
        }
    }

    @FXML
    fun initialize() {
        drawHistogram() // Draw initial empty histogram
    }

    private fun drawHistogram() {
        val gc: GraphicsContext = histogramCanvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, histogramCanvas.width, histogramCanvas.height)

        if (histogramData == null || histogramData!!.isEmpty()) {
            return
        }

        val width = histogramCanvas.width
        val height = histogramCanvas.height
        val barWidth = width / 256.0

        val redData = histogramData!![0] ?: IntArray(256)
        val greenData = histogramData!![1] ?: IntArray(256)
        val blueData = histogramData!![2] ?: IntArray(256)

        val maxCount = (redData + greenData + blueData).maxOrNull() ?: 0

        // Draw Red Histogram
        gc.fill = Color.RED
        for (i in 0 until 256) {
            val barHeight = (redData[i].toDouble() / maxCount * height)
            gc.fillRect(i * barWidth, height - barHeight, barWidth.coerceAtLeast(1.0), barHeight)
        }

        // Draw Green Histogram
        gc.fill = Color.GREEN
        for (i in 0 until 256) {
            val barHeight = (greenData[i].toDouble() / maxCount * height)
            gc.fillRect(i * barWidth, height - barHeight, barWidth.coerceAtLeast(1.0), barHeight)
        }

        // Draw Blue Histogram
        gc.fill = Color.BLUE
        for (i in 0 until 256) {
            val barHeight = (blueData[i].toDouble() / maxCount * height)
            gc.fillRect(i * barWidth, height - barHeight, barWidth.coerceAtLeast(1.0), barHeight)
        }
    }
}
