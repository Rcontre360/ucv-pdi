package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.pdi.core.AppState

class QuantizationPanelController {

    @FXML
    private lateinit var methodComboBox: ComboBox<String>

    @FXML
    private lateinit var kmeansControls: VBox

    @FXML
    private lateinit var kSlider: Slider

    @FXML
    private lateinit var kValueLabel: Label

    private lateinit var appState: AppState

    @FXML
    fun initialize() {
        methodComboBox.items.addAll("K-Means")
        methodComboBox.selectionModel.select("K-Means") // Default to K-Means

        // K-Means specific controls logic
        methodComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            kmeansControls.isVisible = newValue == "K-Means"
        }

        kSlider.valueProperty().addListener { _, _, newValue ->
            kValueLabel.text = newValue.toInt().toString()
        }
    }

    fun setup(appState: AppState) {
        this.appState = appState
        // Potentially load initial values from appState if needed
    }

    @FXML
    fun applyQuantization() {
        val k = kSlider.value.toInt()
        appState.applyKMeansQuantization(k)
        cancel()
    }

    @FXML
    fun cancel() {
        (methodComboBox.scene.window as Stage).close()
    }
}
