package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
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

    // Grouping the checkbox and its label for clean visibility toggling
    @FXML private lateinit var visualizationBox: VBox
    @FXML private lateinit var phaseCheckBox: CheckBox

    private lateinit var appState: AppState
    private lateinit var frequencyImage: Image

    @FXML
    fun initialize() {
        filterTypeComboBox.items.addAll(FilterType.entries)
        filterTypeComboBox.selectionModel.select(FilterType.LOW_PASS)

        domainComboBox.items.addAll(FrequencyDomain.entries)
        domainComboBox.selectionModel.select(FrequencyDomain.DFT)

        // Make the visualization box "un-manageable" when invisible (removes the empty gap)
        visualizationBox.managedProperty().bind(visualizationBox.visibleProperty())

        filterTypeComboBox.valueProperty().addListener { _, _, _ -> updateFilterPreview() }

        // Update display and toggle whole visualization section when domain changes
        domainComboBox.valueProperty().addListener { _, _, newValue ->
            visualizationBox.isVisible = (newValue == FrequencyDomain.DFT)
            updateFrequencyDisplay()
        }

        // Listener for the Phase toggle
        phaseCheckBox.selectedProperty().addListener { _, _, _ -> updateFrequencyDisplay() }

        thresholdSlider.valueProperty().addListener { _, _, newValue ->
            thresholdLabel.text = "Threshold: ${newValue.toInt()}%"
            updateFilterPreview()
        }
    }

    fun setup(appState: AppState) {
        this.appState = appState
        // Initial visibility check for the whole box
        visualizationBox.isVisible = (domainComboBox.value == FrequencyDomain.DFT)
        updateFrequencyDisplay()
        updateFilterPreview()
    }

    private fun updateFrequencyDisplay() {
        if (::frequencyImage.isInitialized) frequencyImage.close()

        val isDFT = domainComboBox.value == FrequencyDomain.DFT
        val space = if (isDFT) DFT() else DCT()

        // If DFT and "Phase" is checked, we show the phase; otherwise magnitude
        frequencyImage = if (isDFT && phaseCheckBox.isSelected) {
            appState.context.currentImage!!.phaseImage(space as DFT)
        } else {
            appState.context.currentImage!!.frequencyImage(space)
        }

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