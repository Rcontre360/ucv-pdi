package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.image.ImageView
import javafx.stage.Stage
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.pdi.core.AppState
import org.pdi.core.FilterType
import org.pdi.core.image.Image
import org.pdi.core.image.createFilterMask
import org.pdi.core.image.toBufferedImage

class DFTPreviewPanelController {

    @FXML
    private lateinit var dftImagePanel: ImageView

    @FXML
    private lateinit var filterMaskPreview: ImageView

    @FXML
    private lateinit var filterTypeComboBox: ComboBox<FilterType>

    @FXML
    private lateinit var thresholdSlider: Slider

    @FXML
    private lateinit var thresholdLabel: Label

    @FXML
    private lateinit var cancelButton: Button

    private lateinit var appState: AppState
    private lateinit var dftImage: Image

    @FXML
    fun initialize() {
        filterTypeComboBox.items.addAll(FilterType.LOW_PASS, FilterType.HIGH_PASS)
        filterTypeComboBox.selectionModel.select(FilterType.LOW_PASS)

        filterTypeComboBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updateFilterPreview()
        }

        thresholdSlider.valueProperty().addListener { _, _, newValue ->
            thresholdLabel.text = "Threshold: ${newValue.toInt()}%"
            updateFilterPreview()
        }
    }

    fun setup(appState: AppState, dftImage: Image) {
        this.appState = appState
        this.dftImage = dftImage

        dftImagePanel.image = javafx.embed.swing.SwingFXUtils.toFXImage(dftImage.image.toBufferedImage(), null)
        updateFilterPreview()
    }

    private fun updateFilterPreview() {
        if (!::dftImage.isInitialized) return

        val filterType = filterTypeComboBox.value
        val threshold = thresholdSlider.value / 100.0
        val isInverted = filterType == FilterType.HIGH_PASS

        val mask = createFilterMask(dftImage.metadata.width, dftImage.metadata.height, threshold, isInverted)

        val displayMask = Mat()
        // Convert the CV_32F mask (with values 0.0 or 1.0) to an 8-bit image for display
        // We scale by 255.0 to map 1.0 to 255 (white).
        mask.convertTo(displayMask, CvType.CV_8U, 255.0)

        filterMaskPreview.image = javafx.embed.swing.SwingFXUtils.toFXImage(displayMask.toBufferedImage(), null)

        mask.release()
        displayMask.release()
    }

    @FXML
    fun applyFilter() {
        val filterType = filterTypeComboBox.value
        val threshold = thresholdSlider.value / 100.0
        appState.applyDFTFilter(filterType, threshold)
        cancel()
    }

    @FXML
    fun cancel() {
        val stage = cancelButton.scene.window as Stage
        stage.close()
    }
}