package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.image.ImageView
import javafx.stage.Stage
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.pdi.core.AppState
import org.pdi.core.FilterType
import org.pdi.io.toBufferedImage

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
            updateFilterPreview()
        }

        thresholdSlider.valueProperty().addListener { _, _, newValue ->
            thresholdLabel.text = "Threshold: ${newValue.toInt()}%"
            updateFilterPreview()
        }
    }

    fun setup(appState: AppState, dftImage: Mat) {
        this.appState = appState
        this.dftImage = dftImage

        dftImagePanel.image = javafx.embed.swing.SwingFXUtils.toFXImage(dftImage.toBufferedImage(), null)
        updateFilterPreview()
    }

    private fun updateFilterPreview() {
        if (!::dftImage.isInitialized) return

        val filterType = filterTypeComboBox.value
        val threshold = thresholdSlider.value / 100.0
        val isInverted = filterType == FilterType.HIGH_PASS

        val mask = org.pdi.core.createFilterMask(dftImage.width(), dftImage.height(), threshold, isInverted)

        val displayMask = Mat()
        Core.normalize(mask, displayMask, 0.0, 255.0, Core.NORM_MINMAX)
        displayMask.convertTo(displayMask, CvType.CV_8U)

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