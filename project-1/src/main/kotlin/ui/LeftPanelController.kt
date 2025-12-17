package org.pdi.ui

import javafx.fxml.FXML
import javafx.scene.control.ColorPicker
import javafx.scene.control.Label
import javafx.scene.control.Slider
import org.pdi.core.AppState
import org.pdi.ui.panels.InfoPanelController

// left panel has the basic functionality we worked first
// has info about the image, brightness/contrast change and grayscale. Also coloring
class LeftPanelController {

    @FXML
    private lateinit var brightnessValueLabel: Label

    @FXML
    private lateinit var brightnessSlider: Slider

    @FXML
    private lateinit var contrastValueLabel: Label

    @FXML
    private lateinit var contrastSlider: Slider

    @FXML
    private lateinit var infoPanelController: InfoPanelController

    @FXML
    private lateinit var colorPicker: ColorPicker

    private lateinit var appState: AppState
    
    fun setAppState(appState: AppState) {
        this.appState = appState

        infoPanelController.setAppState(appState)

        brightnessSlider.valueProperty().addListener { _, _, newValue ->
            val brightness = newValue.toFloat() / 100.0f
            appState.setBrightness(brightness)
        }

        contrastSlider.valueProperty().addListener { _, _, newValue ->
            val contrast = newValue.toFloat() / 100.0f
            appState.setContrast(contrast)
        }

        appState.addContextListener { context ->
            brightnessSlider.value = context.brightness * 100.0
            brightnessValueLabel.text = "Brightness: %.2f".format(context.brightness)

            contrastSlider.value = context.contrast * 100.0
            contrastValueLabel.text = "Contrast: %.2f".format(context.contrast)
        }
    }

    @FXML
    fun applyGrayscale() {
        val selectedColor = colorPicker.value
        val awtColor = java.awt.Color(
            selectedColor.red.toFloat(),
            selectedColor.green.toFloat(),
            selectedColor.blue.toFloat(),
            selectedColor.opacity.toFloat()
        )
        appState.applyGrayscale(awtColor)
    }

    @FXML
    fun applyNegative() {
        appState.applyNegative()
    }
}
