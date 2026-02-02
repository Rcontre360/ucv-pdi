package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.pdi.core.AppState
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType

class QuantizationPanelController {

    @FXML
    private lateinit var methodComboBox: ComboBox<String>

    @FXML
    private lateinit var kmeansControls: VBox

    @FXML
    private lateinit var kSpinner: Spinner<Int> // Spinner for K-Means k value

    @FXML
    private lateinit var uniformQuantizationControls: VBox // Container for Uniform Quantization controls

    @FXML
    private lateinit var bitsSpinner: Spinner<Int> // Spinner for bits per channel

    @FXML
    private lateinit var uniqueColorsLabel: Label // K-Means specific label

    private lateinit var appState: AppState

    @FXML
    fun initialize() {
        // Initialize K-Means Spinner
        kSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(2, 256, 8)
        kSpinner.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) kSpinner.increment(0) // This force-syncs the typed text to the value
        }

        // Initialize Bits Spinner
        bitsSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(0, 8, 8)
        bitsSpinner.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) bitsSpinner.increment(0) // This force-syncs the typed text to the value
        }

        methodComboBox.items.addAll("K-Means", "Uniform Quantization", "Median Cut")
        methodComboBox.selectionModel.select("K-Means") // Default to K-Means

        // Listener to show/hide controls based on selected method
        methodComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            kmeansControls.isVisible = (newValue == "K-Means" || newValue == "Median Cut")
            uniformQuantizationControls.isVisible = newValue == "Uniform Quantization"
        }
    }

    fun setup(appState: AppState) {
        this.appState = appState
        // Get unique colors from the current image metadata for K-Means
        val uniqueColors = appState.context.currentImage?.metadata?.uniqueColors ?: 256
        uniqueColorsLabel.text = "Image has $uniqueColors unique colors"

        // Set the kSpinner's max value to the number of unique colors, ensuring it's at least 2
        val kValueFactory = kSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory
        kValueFactory.max = uniqueColors
        kValueFactory.min = 2
        kValueFactory.value = minOf(64, uniqueColors).coerceAtLeast(2) // Default K to 64 or uniqueColors
    }

    @FXML
    fun applyQuantization() {
        when (methodComboBox.value) {
            "K-Means" -> {
                val k = kSpinner.value
                val uniqueColors = appState.context.currentImage?.metadata?.uniqueColors ?: 256
                if (k < 2 || k > uniqueColors) {
                    showAlert("Invalid Input", "Number of colors (K) must be between 2 and $uniqueColors.")
                    return
                }
                appState.applyKMeansQuantization(k)
            }
            "Uniform Quantization" -> {
                val bits = bitsSpinner.value
                if (bits < 0 || bits > 8) {
                    showAlert("Invalid Input", "Bits per channel must be between 0 and 8.")
                    return
                }
                appState.applyUniformQuantization(bits)
            }
            "Median Cut" -> {
                val k = kSpinner.value
                val uniqueColors = appState.context.currentImage?.metadata?.uniqueColors ?: 256
                if (k < 2 || k > uniqueColors) {
                    showAlert("Invalid Input", "Number of colors (K) must be between 2 and $uniqueColors.")
                    return
                }
                appState.applyMedianCutQuantization(k)
            }
            else -> {
                showAlert("Error", "Please select a quantization method.")
                return
            }
        }
        cancel()
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }

    @FXML
    fun cancel() {
        (methodComboBox.scene.window as Stage).close()
    }
}
