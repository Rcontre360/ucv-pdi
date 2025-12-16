package org.pdi.ui

import javafx.fxml.FXML
import javafx.scene.control.Label
import org.pdi.core.AppState

import javafx.collections.FXCollections
import javafx.scene.control.ComboBox
import org.pdi.core.ZoomAlgorithm

class BottomPanelController {

    @FXML
    private lateinit var zoomLabel: Label

    @FXML
    private lateinit var zoomAlgorithmComboBox: ComboBox<ZoomAlgorithm>

    private lateinit var appState: AppState

    fun setAppState(appState: AppState) {
        this.appState = appState

        zoomAlgorithmComboBox.items.addAll(FXCollections.observableArrayList(ZoomAlgorithm.values().toList()))
        zoomAlgorithmComboBox.selectionModel.select(appState.zoomAlgorithm)
        zoomAlgorithmComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                appState.zoomAlgorithm = newValue
            }
        }

        appState.addContextListener { context ->
            val newFactor = appState.zoomLevels[context.currentZoomLevelIndex]
            zoomLabel.text = "x${"%.1f".format(newFactor)}"
        }
        // Initialize zoom label with current value
        val initialFactor = appState.zoomLevels[appState.context.currentZoomLevelIndex]
        zoomLabel.text = "x${"%.1f".format(initialFactor)}"
    }

    @FXML
    fun zoomIn() {
        appState.zoomIn()
    }

    @FXML
    fun zoomOut() {
        appState.zoomOut()
    }

    @FXML
    fun rotate90() {
        appState.rotate(90)
    }

    @FXML
    fun rotateNeg90() {
        appState.rotate(-90)
    }
}
