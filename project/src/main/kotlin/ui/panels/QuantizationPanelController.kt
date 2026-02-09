package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.core.quantization.*

enum class QuantizationMethod(val displayName: String) {
    K_MEANS("K-Means"),
    MEDIAN_CUT("Median Cut"),
    OCTREE("Octree Quantization");

    override fun toString(): String = displayName
}

class QuantizationPanelController {

    @FXML private lateinit var methodComboBox: ComboBox<QuantizationMethod>
    @FXML private lateinit var colorsSpinner: Spinner<Int>
    @FXML private lateinit var uniqueColorsLabel: Label
    @FXML private lateinit var applyButton: Button

    private lateinit var appState: AppState

    @FXML
    fun initialize() {
        colorsSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(2, 256, 16)
        colorsSpinner.focusedProperty().addListener { _, _, isFocused ->
            if (!isFocused) colorsSpinner.increment(0)
        }

        methodComboBox.items.addAll(QuantizationMethod.entries)
        methodComboBox.selectionModel.select(QuantizationMethod.K_MEANS)
    }

    fun setup(appState: AppState) {
        this.appState = appState
        val uniqueColors = appState.context.currentImage?.metadata?.uniqueColors ?: 256
        uniqueColorsLabel.text = "Image has $uniqueColors unique colors"

        val factory = colorsSpinner.valueFactory as SpinnerValueFactory.IntegerSpinnerValueFactory
        factory.max = uniqueColors
        factory.value = minOf(32, uniqueColors).coerceAtLeast(2)
    }

    @FXML
    fun applyQuantization() {
        val targetColors = colorsSpinner.value
        val selectedMethod = methodComboBox.value ?: return

        val quantizer = when (selectedMethod) {
            QuantizationMethod.K_MEANS -> KMeansQuantizer(targetColors)
            QuantizationMethod.MEDIAN_CUT -> MedianCutQuantizer(targetColors)
            QuantizationMethod.OCTREE -> OctreeQuantizer(targetColors)
        }

        appState.applyQuantization(quantizer)
        cancel()
    }

    @FXML
    fun cancel() {
        (applyButton.scene.window as Stage).close()
    }
}