package org.pdi.ui

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.fxml.FXML
import javafx.scene.control.ColorPicker
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.util.Duration
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

    private var brightnessTimeline: Timeline? = null
    private var contrastTimeline: Timeline? = null
    
    fun setAppState(appState: AppState) {
        this.appState = appState

        infoPanelController.setAppState(appState)

        brightnessSlider.valueProperty().addListener { _, _, newValue ->
            val brightness = newValue.toFloat() / 100.0f
            brightnessValueLabel.text = "Brightness: %.2f".format(brightness) // Update label immediately

            brightnessTimeline?.stop()
            brightnessTimeline = Timeline(KeyFrame(Duration.millis(200.0), { // Debounce for 200ms
                appState.setBrightness(brightness)
            }))
            brightnessTimeline?.play()
        }

        contrastSlider.valueProperty().addListener { _, _, newValue ->
            val contrast = newValue.toFloat() / 100.0f
            contrastValueLabel.text = "Contrast: %.2f".format(contrast) // Update label immediately

            contrastTimeline?.stop()
            contrastTimeline = Timeline(KeyFrame(Duration.millis(200.0), { // Debounce for 200ms
                appState.setContrast(contrast)
            }))
            contrastTimeline?.play()
        }

        appState.addContextListener { context ->
            // Only update sliders if they are not currently being dragged to avoid conflicts
            if (!brightnessSlider.isPressed) {
                brightnessSlider.value = context.brightness * 100.0
                brightnessValueLabel.text = "Brightness: %.2f".format(context.brightness)
            }

            if (!contrastSlider.isPressed) {
                contrastSlider.value = context.contrast * 100.0
                contrastValueLabel.text = "Contrast: %.2f".format(context.contrast)
            }
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
