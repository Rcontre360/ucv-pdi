package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.stage.Stage
import org.pdi.core.AppState

enum class ThresholdType { OTSU, TRIANGLE }

class UmbralizationPanelController {

    @FXML
    private lateinit var algorithmComboBox: ComboBox<ThresholdType>

    private lateinit var appState: AppState
    private var onApply: (() -> Unit)? = null

    @FXML
    fun initialize() {
        // Populate the dropdown with the Otsu and Triangle options
        algorithmComboBox.items.addAll(ThresholdType.entries)
        algorithmComboBox.selectionModel.select(ThresholdType.OTSU)
    }

    fun setup(appState: AppState, onApply: () -> Unit) {
        this.appState = appState
        this.onApply = onApply
    }

    @FXML
    fun applyThresholding() {
        // Retrieve the selection (0 for Otsu, 1 for Triangle)
        val selectedType = algorithmComboBox.value

        // Pass the ID to AppState which will call the makeThreshold(type) logic
        appState.applyThresholding(1)

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