package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.image.ImageView
import javafx.stage.Stage
import org.opencv.core.Mat
import org.pdi.core.AppState
import org.pdi.io.toBufferedImage

enum class FilterType {
    LOW_PASS,
    HIGH_PASS
}

class DFTPreviewPanelController {

    @FXML
    private lateinit var dftImagePanel: ImageView

    @FXML
    private lateinit var filterTypeComboBox: ComboBox<FilterType>

    @FXML
    private lateinit var thresholdSlider: Slider

    @FXML
    private lateinit var thresholdLabel: Label

    @FXML
    private lateinit var applyButton: Button

    @FXML
    private lateinit var cancelButton: Button

    private lateinit var appState: AppState
    private lateinit var dftImage: Mat

    @FXML
    fun initialize() {
        filterTypeComboBox.items.addAll(FilterType.LOW_PASS, FilterType.HIGH_PASS)
        filterTypeComboBox.selectionModel.select(FilterType.LOW_PASS)

        filterTypeComboBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->

        }

        thresholdSlider.valueProperty().addListener { _, _, newValue ->
            thresholdLabel.text = "Threshold: ${newValue.toInt()}%"
        }
    }

    fun setup(appState: AppState, dftImage: Mat) {
        this.appState = appState
        this.dftImage = dftImage

        dftImagePanel.image =javafx.embed.swing.SwingFXUtils.toFXImage(dftImage.toBufferedImage(), null)
    }

    @FXML
    fun applyFilter() {
        val filterType = filterTypeComboBox.value
        val threshold = thresholdSlider.value.toFloat()
        //appState.applyDTFFilter(filterType, threshold, dftData)
        cancel()
    }

    @FXML
    fun cancel() {
        val stage = cancelButton.scene.window as Stage
        stage.close()
    }
}