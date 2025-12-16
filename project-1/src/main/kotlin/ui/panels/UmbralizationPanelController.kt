package org.pdi.ui.panels

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.pdi.core.AppState
import kotlin.math.abs

class UmbralizationPanelController {

    @FXML
    private lateinit var gradientCanvas: Canvas

    @FXML
    private lateinit var thresholdListView: ListView<Int>

    @FXML
    private lateinit var removeThresholdButton: Button

    private lateinit var appState: AppState
    private var onApply: (() -> Unit)? = null
    private val thresholds = FXCollections.observableArrayList<Int>()
    private var draggedThresholdIndex: Int = -1
    private val HIT_TOLERANCE = 5

    @FXML
    fun initialize() {
        thresholdListView.items = thresholds
        thresholdListView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            removeThresholdButton.isDisable = newValue == null
        }
        removeThresholdButton.isDisable = true

        gradientCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED) { event ->
            handleMousePressed(event)
        }
        gradientCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED) { event ->
            handleMouseDragged(event)
        }
        gradientCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED) { event ->
            handleMouseReleased(event)
        }
    }

    fun setup(appState: AppState, onApply: () -> Unit) {
        this.appState = appState
        this.onApply = onApply
        refreshUI()
        addThreshold()
        drawGradient()
    }

    private fun handleMousePressed(event: MouseEvent) {
        val x = event.x.toInt()

        if (event.button == MouseButton.SECONDARY) { // Right-click to remove
            val thresholdIndexToRemove = findNearbyThreshold(x)
            if (thresholdIndexToRemove != -1) {
                thresholds.removeAt(thresholdIndexToRemove)
                refreshUI()
            }
            return
        }

        draggedThresholdIndex = findNearbyThreshold(x)
        if (draggedThresholdIndex != -1) {
            // Cursor change is handled by CSS or directly on scene
            thresholdListView.selectionModel.select(thresholds[draggedThresholdIndex])
        }

        if (event.clickCount == 2 && draggedThresholdIndex == -1) { // Double-click to add
            val newThreshold = ((x.toDouble() / (gradientCanvas.width - 1)) * 255).toInt().coerceIn(0, 255)
            if (thresholds.none { abs(it - newThreshold) < HIT_TOLERANCE * 2 }) {
                thresholds.add(newThreshold)
                refreshUI()
                thresholdListView.selectionModel.select(newThreshold)
            }
        }
    }

    private fun handleMouseDragged(event: MouseEvent) {
        if (draggedThresholdIndex != -1) {
            var newThreshold = ((event.x.toDouble() / (gradientCanvas.width - 1)) * 255).toInt()

            val sortedThresholds = thresholds.sorted()
            val currentThresholdValue = thresholds[draggedThresholdIndex]
            val currentThresholdSortedIndex = sortedThresholds.indexOf(currentThresholdValue)

            val lowerBound = if (currentThresholdSortedIndex > 0) sortedThresholds[currentThresholdSortedIndex - 1] + 1 else 0
            val upperBound = if (currentThresholdSortedIndex < sortedThresholds.size - 1) sortedThresholds[currentThresholdSortedIndex + 1] - 1 else 255

            val clampedValue = newThreshold.coerceIn(lowerBound, upperBound)

            if (thresholds[draggedThresholdIndex] != clampedValue) {
                thresholds[draggedThresholdIndex] = clampedValue
                refreshUI()
                thresholdListView.selectionModel.select(clampedValue)
            }
        }
    }

    private fun handleMouseReleased(event: MouseEvent) {
        draggedThresholdIndex = -1
    }

    private fun findNearbyThreshold(x: Int): Int {
        for ((index, threshold) in thresholds.withIndex()) {
            val thresholdX = (threshold / 255.0 * (gradientCanvas.width - 1)).toInt()
            if (abs(x - thresholdX) <= HIT_TOLERANCE) {
                return index
            }
        }
        return -1
    }

    @FXML
    fun addThreshold() {
        if (thresholds.size >= 255) return // Full

        val sortedThresholds = thresholds.sorted()
        val points = (listOf(0) + sortedThresholds + listOf(255)).sorted()
        var maxGap = 0
        var gapStartIndex = 0

        for (i in 0 until points.size - 1) {
            val gap = points[i + 1] - points[i]
            if (gap > maxGap) {
                maxGap = gap
                gapStartIndex = i
            }
        }

        if (maxGap > 1) {
            val newThreshold = points[gapStartIndex] + maxGap / 2
            if (!thresholds.contains(newThreshold)) {
                thresholds.add(newThreshold)
                refreshUI()
                thresholdListView.selectionModel.select(newThreshold)
            }
        }
    }

    @FXML
    fun removeThreshold() {
        val selectedValue = thresholdListView.selectionModel.selectedItem
        if (selectedValue != null) {
            thresholds.remove(selectedValue)
            refreshUI()
        }
    }

    @FXML
    fun applyThresholding() {
        if (!appState.isCurrentImageGrayscale()) {
            showAlert("Grayscale Required", "Please apply grayscale filter first.")
            return
        }
        appState.applyThresholding(thresholds.toList())
        onApply?.invoke()
    }

    private fun refreshUI() {
        thresholds.sort()
        drawGradient()
    }

    private fun drawGradient() {
        val gc: GraphicsContext = gradientCanvas.graphicsContext2D
        val width = gradientCanvas.width
        val height = gradientCanvas.height

        if (width <= 0 || height <= 0) {
            return
        }

        // Draw grayscale gradient
        for (i in 0 until width.toInt()) {
            val gray = (i.toFloat() / (width - 1) * 255).toInt()
            gc.fill = Color.rgb(gray, gray, gray)
            gc.fillRect(i.toDouble(), 0.0, 1.0, height)
        }

        // Draw threshold lines
        for (threshold in thresholds) {
            val x = (threshold / 255.0 * (width - 1)).toInt()
            gc.stroke = Color.RED
            gc.strokeLine(x.toDouble(), 0.0, x.toDouble(), height)
            // Draw a handle
            gc.fill = Color.WHITE
            gc.fillOval(x - 3.0, height / 2 - 3.0, 6.0, 6.0)
            gc.stroke = Color.BLACK
            gc.strokeOval(x - 3.0, height / 2 - 3.0, 6.0, 6.0)
        }
    }

    private fun showAlert(title: String, message: String) {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
