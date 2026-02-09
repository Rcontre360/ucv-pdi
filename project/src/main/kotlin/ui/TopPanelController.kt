package org.pdi.ui

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.stage.Modality
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.io.lastUsedDirectory
import org.pdi.ui.panels.*
import java.awt.Color

class TopPanelController {

    private lateinit var appState: AppState
    private lateinit var primaryStage: Stage

    // Caché para evitar duplicar ventanas abiertas
    private val activePanels = mutableMapOf<String, Stage>()

    fun setAppState(appState: AppState, primaryStage: Stage) {
        this.appState = appState
        this.primaryStage = primaryStage
    }

    private fun showPanel(fxmlPath: String, title: String, setupController: (Any) -> Unit) {
        // Si la ventana ya existe y está visible, traerla al frente
        activePanels[fxmlPath]?.let {
            if (it.isShowing) {
                it.toFront()
                return
            }
        }

        val loader = FXMLLoader(javaClass.getResource(fxmlPath))
        val root = loader.load<Parent>()
        setupController(loader.getController())

        val stage = Stage()
        stage.initOwner(primaryStage)
        stage.initModality(Modality.NONE) // No bloquea la ventana principal
        stage.title = title
        stage.scene = Scene(root)

        // Limpiar del mapa cuando se cierre
        stage.setOnCloseRequest { activePanels.remove(fxmlPath) }

        activePanels[fxmlPath] = stage
        stage.show()
    }

    fun saveImage() {
        showPanel("/panels/SaveImagePanel.fxml", "Save Image") { controller ->
            (controller as SaveImagePanelController).setup(appState) {
                activePanels["/panels/SaveImagePanel.fxml"]?.close()
            }
        }
    }

    fun selectImage() {
        val fileChooser = javafx.stage.FileChooser()
        lastUsedDirectory?.takeIf { it.exists() }?.let {
            fileChooser.initialDirectory = it
        }
        fileChooser.title = "Select Image"
        val file = fileChooser.showOpenDialog(primaryStage)
        if (file != null) {
            appState.loadImage(file)
        }
    }

    fun showHistogram() {
        if (appState.getHistogram() == null) {
            showAlert("No Image Selected", "No image loaded.")
            return
        }
        showPanel("/panels/HistogramPanel.fxml", "Histogram") {
            (it as HistogramPanelController).setAppState(appState)
        }
    }

    fun showTonalCurve() {
        if (appState.getTonalCurve() == null) {
            showAlert("No Image Selected", "No image loaded or curve data unavailable.")
            return
        }
        showPanel("/panels/TonalCurvePanel.fxml", "Tonal Curve Viewer") {
            (it as TonalCurvePanelController).initialize(appState)
        }
    }

    fun showUmbralization() {
        if (!appState.isCurrentImageGrayscale()) {
            showAlert("Grayscale Required", "Please apply grayscale filter first.")
            return
        }
        showPanel("/panels/UmbralizationPanel.fxml", "Umbralization") { controller ->
            (controller as UmbralizationPanelController).setup(appState) {
                activePanels["/panels/UmbralizationPanel.fxml"]?.close()
            }
        }
    }

    fun showLineProfile() {
        if (appState.getImage() == null) {
            showAlert("No Image Selected", "No image loaded.")
            return
        }
        showPanel("/panels/LineProfilePanel.fxml", "Line Profile") {
            (it as LineProfilePanelController).setAppState(appState)
        }
    }

    @FXML
    fun showRegionGrowingPanel() {
        val currentImage = appState.context.currentImage ?: return showAlert("No Image", "No image loaded.")
        val grayscaleImage = currentImage.toGrayscale(Color.WHITE)

        showPanel("/panels/RegionGrowingPanel.fxml", "Region Growing") {
            (it as RegionGrowingPanelController).initialize(appState, grayscaleImage)
        }
    }

    @FXML
    fun applyDFT() {
        if (appState.context.currentImage == null) return showAlert("No Image", "No image loaded.")
        showPanel("/panels/DFTPreviewPanel.fxml", "DFT Preview and Filter") {
            (it as DFTPreviewPanelController).setup(appState)
        }
    }

    @FXML
    fun showQuantizationPanel() {
        if (appState.context.currentImage == null) return showAlert("No Image", "No image loaded.")
        showPanel("/panels/QuantizationPanel.fxml", "Quantization") {
            (it as QuantizationPanelController).setup(appState)
        }
    }

    @FXML fun clearImage() = appState.clear()
    @FXML fun undo() = appState.undo()
    @FXML fun redo() = appState.redo()

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}