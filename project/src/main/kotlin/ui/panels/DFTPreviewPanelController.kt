package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.stage.Stage
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.pdi.core.AppState
import org.pdi.core.FilterType
import org.pdi.core.image.Image
import org.pdi.core.image.createFilterMask
import org.pdi.core.image.toBufferedImage

enum class FrequencyDomain { DFT, DCT }

class DFTPreviewPanelController {

    @FXML private lateinit var dftImagePanel: ImageView
    @FXML private lateinit var filterMaskPreview: ImageView
    @FXML private lateinit var filterTypeComboBox: ComboBox<FilterType>
    @FXML private lateinit var domainComboBox: ComboBox<FrequencyDomain> // El nuevo selector
    @FXML private lateinit var thresholdSlider: Slider
    @FXML private lateinit var thresholdLabel: Label
    @FXML private lateinit var cancelButton: Button

    private lateinit var appState: AppState
    private lateinit var frequencyImage: Image

    @FXML
    fun initialize() {
        filterTypeComboBox.items.addAll(FilterType.LOW_PASS, FilterType.HIGH_PASS)
        filterTypeComboBox.selectionModel.select(FilterType.LOW_PASS)

        domainComboBox.items.addAll(FrequencyDomain.DFT, FrequencyDomain.DCT)
        domainComboBox.selectionModel.select(FrequencyDomain.DFT)

        filterTypeComboBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updateFilterPreview()
        }

        thresholdSlider.valueProperty().addListener { _, _, newValue ->
            thresholdLabel.text = "Threshold: ${newValue.toInt()}%"
            updateFilterPreview()
        }

        domainComboBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updateFrequencyDisplay()
        }
    }

    fun setup(appState: AppState) {
        this.appState = appState
        updateFrequencyDisplay()
        updateFilterPreview()
    }

    private fun updateFrequencyDisplay() {
        if (::frequencyImage.isInitialized) frequencyImage.close()

        val selectedDomain = domainComboBox.value

        frequencyImage = if (selectedDomain == FrequencyDomain.DFT) {
            appState.context.currentImage!!.dftImage()
        } else {
            appState.context.currentImage!!.dctImage()
        }

        println("UPDATED FREQUENCY DISPLAY TO $selectedDomain")

        dftImagePanel.image = javafx.embed.swing.SwingFXUtils.toFXImage(frequencyImage.image.toBufferedImage(), null)
    }

    private fun updateFilterPreview() {
        if (!::frequencyImage.isInitialized) return

        val filterType = filterTypeComboBox.value
        val threshold = thresholdSlider.value / 100.0
        val isInverted = filterType == FilterType.HIGH_PASS

        val width = appState.context.currentImage?.metadata?.width ?: frequencyImage.metadata.width
        val height = appState.context.currentImage?.metadata?.height ?: frequencyImage.metadata.height

        val mask = createFilterMask(width, height, threshold, isInverted)
        val displayMask = Mat()

        mask.convertTo(displayMask, CvType.CV_8U, 255.0)
        filterMaskPreview.image = javafx.embed.swing.SwingFXUtils.toFXImage(displayMask.toBufferedImage(), null)

        mask.release()
        displayMask.release()
    }

    @FXML
    fun applyFilter() {
        val filterType = filterTypeComboBox.value
        val threshold = thresholdSlider.value / 100.0
        // Nota: Deberías asegurarte de que appState sepa si aplicar DFT o DCT según el combo
        appState.applyDFTFilter(filterType, threshold)
        cancel()
    }

    @FXML
    fun cancel() {
        if (::frequencyImage.isInitialized) frequencyImage.close()
        val stage = cancelButton.scene.window as Stage
        stage.close()
    }
}