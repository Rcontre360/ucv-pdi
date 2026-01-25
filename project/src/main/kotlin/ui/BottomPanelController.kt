package org.pdi.ui

import javafx.fxml.FXML
import javafx.scene.control.Label
import org.pdi.core.AppState

import javafx.collections.FXCollections
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import org.pdi.core.ZoomAlgorithm

// here in the bottom panel we put all the geometric transformations
class BottomPanelController {

    @FXML
    private lateinit var zoomLabel: Label

    @FXML
    private lateinit var zoomAlgorithmComboBox: ComboBox<ZoomAlgorithm>

    @FXML
    private lateinit var rotationSlider: Slider

    @FXML
    private lateinit var rotationTextField: TextField

    @FXML
    private lateinit var panningCheckBox: CheckBox

    private lateinit var appState: AppState

    fun setAppState(appState: AppState) {
        this.appState = appState

        // zoom selectors
        zoomAlgorithmComboBox.items.addAll(FXCollections.observableArrayList(ZoomAlgorithm.values().toList()))
        zoomAlgorithmComboBox.selectionModel.select(appState.zoomAlgorithm)
        zoomAlgorithmComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                appState.zoomAlgorithm = newValue
            }
        }

        // change the zoom label live
        appState.addContextListener { context ->
            val newFactor = appState.zoomLevels[context.currentZoomLevelIndex]
            zoomLabel.text = "x${"%.1f".format(newFactor)}"
        }
        // Initialize zoom label with current value
        val initialFactor = appState.zoomLevels[appState.context.currentZoomLevelIndex]
        zoomLabel.text = "x${"%.1f".format(initialFactor)}"

        rotationSlider.valueProperty().addListener { _, _, newValue ->
            rotationTextField.text = newValue.toInt().toString()
        }

        rotationSlider.valueChangingProperty().addListener { _, _, isChanging ->
            if (!isChanging) {
                val angle = rotationSlider.value.toInt()
                appState.rotate(angle)
            }
        }

        panningCheckBox.selectedProperty().addListener { _, _, newValue ->
            appState.setPanningMode(newValue)
        }
    }

    @FXML
    fun zoomIn() {
        appState.zoomIn()
    }

    @FXML
    fun zoomOut() {
        appState.zoomOut()
    }
}
