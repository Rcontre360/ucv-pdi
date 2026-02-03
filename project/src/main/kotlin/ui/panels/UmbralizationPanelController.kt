package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.stage.Stage
import org.pdi.core.AppState

enum class ThresholdType(val label: String, val id: Int) {
    OTSU("OpenCV Otsu", 0),
    CUSTOM("Custom", 1);

    override fun toString(): String = label
}

class UmbralizationPanelController {

    @FXML
    private lateinit var algorithmComboBox: ComboBox<ThresholdType>

    private lateinit var appState: AppState
    private var onApply: (() -> Unit)? = null

    @FXML
    fun initialize() {
        // Populate the dropdown with our two types
        algorithmComboBox.items.addAll(ThresholdType.values())
        algorithmComboBox.selectionModel.select(ThresholdType.OTSU)
    }

    fun setup(appState: AppState, onApply: () -> Unit) {
        this.appState = appState
        this.onApply = onApply
    }

    @FXML
    fun applyThresholding() {
        // Get the ID of the selected algorithm (0 for Otsu, 1 for Custom)
        val selectedId = algorithmComboBox.value.id

        // Execute the thresholding in AppState
        appState.applyThresholding(selectedId)

        onApply?.invoke()
        close()
    }

    @FXML
    fun close() {
        val scene = algorithmComboBox.scene
        if (scene != null) {
            val stage = scene.window as Stage
            stage.close()
        }
    }
}