package org.pdi.ui

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.ui.panels.HistogramPanelController
import org.pdi.ui.panels.LineProfilePanelController
import org.pdi.ui.panels.SaveImagePanelController
import org.pdi.ui.panels.TonalCurvePanelController
import org.pdi.ui.panels.UmbralizationPanelController

class TopPanelController {

    private lateinit var appState: AppState

    fun setAppState(appState: AppState) {
        this.appState = appState
    }

    fun saveImage() {
        val loader = FXMLLoader(javaClass.getResource("/SaveImagePanel.fxml"))
        val root = loader.load<Parent>()
        val saveImagePanelController: SaveImagePanelController = loader.getController()
        saveImagePanelController.setup(appState) {
            (root.scene.window as Stage).close()
        }

        val stage = Stage()
        stage.title = "Save Image"
        stage.scene = Scene(root)
        stage.show()
    }

    fun selectImage() {
        val fileChooser = javafx.stage.FileChooser()
        fileChooser.title = "Select Image"
        val file = fileChooser.showOpenDialog(null)
        if (file != null) {
            appState.loadImage(file)
        }
    }

    fun showHistogram() {
        if (appState.getHistogram() == null) {
            showAlert("No Image Selected", "No image loaded.")
            return
        }
        val loader = FXMLLoader(javaClass.getResource("/HistogramPanel.fxml"))
        val root = loader.load<Parent>()
        val histogramPanelController: HistogramPanelController = loader.getController()
        histogramPanelController.setAppState(appState)

        val stage = Stage()
        stage.title = "Histogram"
        stage.scene = Scene(root)
        stage.show()
    }

    fun showTonalCurve() {
        if (appState.getTonalCurve() == null) {
            showAlert("No Image Selected", "No image loaded or curve data unavailable.")
            return
        }
        val loader = FXMLLoader(javaClass.getResource("/TonalCurvePanel.fxml"))
        val root = loader.load<Parent>()
        val tonalCurvePanelController: TonalCurvePanelController = loader.getController()
        tonalCurvePanelController.setAppState(appState)

        val stage = Stage()
        stage.title = "Tonal Curve Viewer"
        stage.scene = Scene(root)
        stage.show()
    }

    fun showUmbralization() {
        if (!appState.isCurrentImageGrayscale()) {
            showAlert("Grayscale Required", "Please apply grayscale filter first.")
            return
        }
        val loader = FXMLLoader(javaClass.getResource("/UmbralizationPanel.fxml"))
        val root = loader.load<Parent>()
        val umbralizationPanelController: UmbralizationPanelController = loader.getController()
        umbralizationPanelController.setup(appState) {
            (root.scene.window as Stage).close()
        }

        val stage = Stage()
        stage.title = "Umbralization"
        stage.scene = Scene(root)
        stage.show()
    }

    fun showLineProfile() {
        if (appState.getImage() == null) {
            showAlert("No Image Selected", "No image loaded.")
            return
        }
        val loader = FXMLLoader(javaClass.getResource("/LineProfilePanel.fxml"))
        val root = loader.load<Parent>()
        val lineProfilePanelController: LineProfilePanelController = loader.getController()
        lineProfilePanelController.setAppState(appState)

        val stage = Stage()
        stage.title = "Line Profile"
        stage.scene = Scene(root)
        stage.show()
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
