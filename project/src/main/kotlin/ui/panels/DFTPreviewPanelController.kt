package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.core.image.Image
import org.pdi.core.image.toBufferedImage
import org.pdi.core.transforms.DCT
import org.pdi.core.transforms.DFT

enum class FrequencyDomain { DFT, DCT }

enum class FilterType { LOW_PASS, HIGH_PASS }

class DFTPreviewPanelController {

    @FXML private lateinit var dftImagePanel: ImageView
    @FXML private lateinit var filterMaskPreview: ImageView
    @FXML private lateinit var filterTypeComboBox: ComboBox<FilterType>
    @FXML private lateinit var domainComboBox: ComboBox<FrequencyDomain>
    @FXML private lateinit var thresholdSlider: Slider
    @FXML private lateinit var thresholdLabel: Label
    @FXML private lateinit var cancelButton: Button

    private lateinit var appState: AppState
    private lateinit var frequencyImage: Image

    @FXML
    fun initialize() {
        filterTypeComboBox.items.addAll(FilterType.values())
        filterTypeComboBox.selectionModel.select(FilterType.LOW_PASS)

        domainComboBox.items.addAll(FrequencyDomain.values())
        domainComboBox.selectionModel.select(FrequencyDomain.DFT)

        filterTypeComboBox.valueProperty().addListener { _, _, _ -> updateFilterPreview() }
        domainComboBox.valueProperty().addListener { _, _, _ -> updateFrequencyDisplay() }

        thresholdSlider.valueProperty().addListener { _, _, newValue ->
            thresholdLabel.text = "Threshold: ${newValue.toInt()}%"
            updateFilterPreview()
        }
    }

    fun setup(appState: AppState) {
        this.appState = appState
        updateFrequencyDisplay()
        updateFilterPreview()
    }

    private fun updateFrequencyDisplay() {
        if (::frequencyImage.isInitialized) frequencyImage.close()

        val space = if (domainComboBox.value == FrequencyDomain.DFT) DFT() else DCT()
        frequencyImage = appState.context.currentImage!!.frequencyImage(space)

        dftImagePanel.image = javafx.embed.swing.SwingFXUtils.toFXImage(frequencyImage.image.toBufferedImage(), null)
    }

    private fun updateFilterPreview() {
        if (!::frequencyImage.isInitialized) return

        val width = appState.context.currentImage!!.metadata.width
        val height = appState.context.currentImage!!.metadata.height

        val space = if (domainComboBox.value == FrequencyDomain.DFT) DFT() else DCT()
        val filter = space.createFilter(height, width, thresholdSlider.value / 100.0, filterTypeComboBox.value == FilterType.HIGH_PASS)

        val displayMask = filter.getAsImage()
        filterMaskPreview.image = javafx.embed.swing.SwingFXUtils.toFXImage(displayMask.toBufferedImage(), null)

        displayMask.release()
    }

    @FXML
    fun applyFilter() {
        val width = appState.context.currentImage!!.metadata.width
        val height = appState.context.currentImage!!.metadata.height
        val space = if (domainComboBox.value == FrequencyDomain.DFT) DFT() else DCT()

        val filter = space.createFilter(height, width, thresholdSlider.value / 100.0, filterTypeComboBox.value == FilterType.HIGH_PASS)

        appState.applyFrequencyFilter(space, filter)
        cancel()
    }

    @FXML
    fun cancel() {
        if (::frequencyImage.isInitialized) frequencyImage.close()
        (cancelButton.scene.window as Stage).close()
    }
}