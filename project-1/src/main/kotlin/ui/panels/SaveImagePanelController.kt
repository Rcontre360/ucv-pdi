package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.io.saveImage
import java.io.File

class SaveImagePanelController {

    @FXML
    private lateinit var fileNameField: TextField

    @FXML
    private lateinit var formatComboBox: ComboBox<String>

    @FXML
    private lateinit var directoryLabel: Label

    private lateinit var appState: AppState
    private var onSave: (() -> Unit)? = null
    private var selectedDirectory: File? = null

    @FXML
    fun initialize() {
        formatComboBox.items.addAll("png", "bmp", "netpbm", "pdi")
        formatComboBox.selectionModel.selectFirst()
    }

    fun setup(appState: AppState, onSave: () -> Unit) {
        this.appState = appState
        this.onSave = onSave
    }

    @FXML
    fun selectDirectory() {
        val directoryChooser = DirectoryChooser()
        directoryChooser.title = "Select a directory"
        val stage = directoryLabel.scene.window as Stage
        selectedDirectory = directoryChooser.showDialog(stage)
        selectedDirectory?.let {
            directoryLabel.text = "Selected Directory: ${it.absolutePath}"
        }
    }

    @FXML
    fun saveImage() {
        val currentImage = appState.context.currentImage
        if (currentImage == null) {
            showAlert("No Image Selected", "No image loaded.")
            return
        }

        if (selectedDirectory == null) {
            showAlert("No Directory Selected", "Please select a directory.")
            return
        }

        val fileName = fileNameField.text
        if (fileName.isBlank()) {
            showAlert("No File Name", "Please enter a file name.")
            return
        }

        val format = formatComboBox.selectionModel.selectedItem
        val fullPath = "${selectedDirectory!!.absolutePath}/$fileName.$format"

        try {
            saveImage(fullPath, format, currentImage)
            showAlert("Success", "Image saved successfully!")
            onSave?.invoke()
        } catch (e: Exception) {
            showAlert("Error", "Error saving image: ${e.message}")
        }
    }

    private fun showAlert(title: String, message: String) {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
