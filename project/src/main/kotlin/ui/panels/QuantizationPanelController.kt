package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.pdi.core.AppState

class QuantizationPanelController {

    @FXML
    private lateinit var methodComboBox: ComboBox<String>

    @FXML
    private lateinit var kmeansControls: VBox

    @FXML
    private lateinit var kSpinner: Spinner<Int>

    @FXML
    private lateinit var kValueLabel: Label

    private lateinit var appState: AppState

    @FXML
    fun initialize() {
        // 1. Initialize the Value Factory (Min: 2, Max: 256, Initial: 8)
        val valueFactory = javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(2, 256, 8)
        kSpinner.valueFactory = valueFactory

        methodComboBox.items.addAll("K-Means")
        methodComboBox.selectionModel.select("K-Means")

        methodComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            kmeansControls.isVisible = newValue == "K-Means"
        }

        // Now newValue will never be null because the factory guarantees a range
        kSpinner.valueProperty().addListener { _, _, newValue ->
            kValueLabel.text = newValue.toString()
        }
        kSpinner.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) kSpinner.increment(0) // This force-syncs the typed text to the value
        }
    }

    fun setup(appState: AppState) {
        this.appState = appState
    }

    @FXML
    fun applyQuantization() {
        val k = kSpinner.value.toInt()
        appState.applyKMeansQuantization(k)
        cancel()
    }

    @FXML
    fun cancel() {
        (methodComboBox.scene.window as Stage).close()
    }
}
