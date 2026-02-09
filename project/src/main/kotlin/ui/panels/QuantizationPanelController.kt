package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.core.quantization.*
import kotlin.math.ln

class QuantizationPanelController {

    @FXML private lateinit var methodComboBox: ComboBox<String>
    @FXML private lateinit var colorsSpinner: Spinner<Int>
    @FXML private lateinit var uniqueColorsLabel: Label
    @FXML private lateinit var applyButton: Button

    private lateinit var appState: AppState

    @FXML
    fun initialize() {
        // Inicializamos con un rango estándar de colores (2 a 256)
        colorsSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(2, 256, 16)

        // Sincronización de texto para el Spinner
        colorsSpinner.focusedProperty().addListener { _, _, isFocused ->
            if (!isFocused) colorsSpinner.increment(0)
        }

        methodComboBox.items.addAll("K-Means", "Uniform Quantization", "Median Cut")
        methodComboBox.selectionModel.select("K-Means")

        // No ocultamos controles porque ahora la entrada es universal (cantidad de colores)
    }

    fun setup(appState: AppState) {
        this.appState = appState
        val uniqueColors = appState.context.currentImage?.metadata?.uniqueColors ?: 256
        uniqueColorsLabel.text = "Image has $uniqueColors unique colors"

        // Ajustamos el límite del spinner según la imagen actual
        val factory = colorsSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory
        factory.max = uniqueColors
        factory.value = minOf(32, uniqueColors).coerceAtLeast(2)
    }

    @FXML
    fun applyQuantization() {
        val targetColors = colorsSpinner.value
        val method = methodComboBox.value

        val quantizer = when (method) {
            "K-Means" -> KMeansQuantizer(targetColors)
            "Median Cut" -> MedianCutQuantizer(targetColors)
            "Uniform Quantization" -> {
                val bits = (ln(targetColors.toDouble()) / ln(2.0)).toInt().coerceIn(1, 8)
                UniformQuantizer(bits)
            }
            else -> return
        }

        appState.applyQuantization(quantizer)
        cancel()
    }

    @FXML
    fun cancel() {
        (applyButton.scene.window as Stage).close()
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}