package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.stage.Stage
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.pdi.core.AppState
import org.pdi.core.image.Image
import org.pdi.core.image.toBufferedImage
import org.pdi.core.transforms.DCT
import org.pdi.core.transforms.DFT
import org.pdi.core.transforms.Transform

enum class FrequencyDomain { DFT, DCT }

enum class FilterType {
    LOW_PASS,
    HIGH_PASS
}

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
        val space = if (selectedDomain == FrequencyDomain.DFT) DFT() else DCT()

        frequencyImage = appState.context.currentImage!!.frequencyImage(space)

        println("UPDATED FREQUENCY DISPLAY TO $selectedDomain")

        dftImagePanel.image = javafx.embed.swing.SwingFXUtils.toFXImage(frequencyImage.image.toBufferedImage(), null)
    }

    private fun updateFilterPreview() {
        if (!::frequencyImage.isInitialized) return

        val threshold = thresholdSlider.value / 100.0
        val isHighPass = filterTypeComboBox.value == FilterType.HIGH_PASS

        val width = appState.context.currentImage?.metadata?.width ?: frequencyImage.metadata.width
        val height = appState.context.currentImage?.metadata?.height ?: frequencyImage.metadata.height

        val space = if (domainComboBox.value == FrequencyDomain.DFT) DFT() else DCT()

        // Obtenemos la instancia del filtro desde la Transform
        val filter = space.createFilter(height, width, threshold, isHighPass)

        // Usamos getAsImage() de la clase base FrequencyFilter
        val displayMask = filter.getAsImage()

        filterMaskPreview.image = javafx.embed.swing.SwingFXUtils.toFXImage(displayMask.toBufferedImage(), null)

        displayMask.release()
    }

    @FXML
    fun applyFilter() {
        val threshold = thresholdSlider.value / 100.0
        val isHighPass = filterTypeComboBox.value == FilterType.HIGH_PASS

        val space = if (domainComboBox.value == FrequencyDomain.DFT) DFT() else DCT()

        val width = appState.context.currentImage?.metadata?.width ?: 0
        val height = appState.context.currentImage?.metadata?.height ?: 0

        // Instanciamos el filtro final
        val filter = space.createFilter(height, width, threshold, isHighPass)

        appState.applyFrequencyFilter(space, filter)
        cancel()
    }

    @FXML
    fun cancel() {
        if (::frequencyImage.isInitialized) frequencyImage.close()
        val stage = cancelButton.scene.window as Stage
        stage.close()
    }
}