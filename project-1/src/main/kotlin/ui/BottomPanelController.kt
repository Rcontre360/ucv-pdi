package org.pdi.ui

import javafx.fxml.FXML
import javafx.scene.control.Label
import org.pdi.core.AppState

class BottomPanelController {

    @FXML
    private lateinit var zoomLabel: Label

    private lateinit var appState: AppState

    fun setAppState(appState: AppState) {
        this.appState = appState

        appState.addContextListener { context ->
            val newFactor = appState.zoomLevels[context.currentZoomLevelIndex]
            zoomLabel.text = "x${"%.1f".format(newFactor)}"
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

    @FXML
    fun rotate90() {
        appState.rotate(90)
    }

    @FXML
    fun rotate180() {
        appState.rotate(180)
    }

    @FXML
    fun rotate270() {
        appState.rotate(270)
    }
}
