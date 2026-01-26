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

    @FXML
    private lateinit var hueValueLabel: Label

    @FXML
    private lateinit var hueSlider: Slider

    @FXML
    private lateinit var saturationValueLabel: Label

    @FXML
    private lateinit var saturationSlider: Slider

    @FXML
    private lateinit var lightnessValueLabel: Label

    @FXML
    private lateinit var lightnessSlider: Slider

    @FXML
    private lateinit var yValueLabel: Label

    @FXML
    private lateinit var ySlider: Slider

    @FXML
    private lateinit var uValueLabel: Label

    @FXML
    private lateinit var uSlider: Slider

    @FXML
    private lateinit var vValueLabel: Label

    @FXML
    private lateinit var vSlider: Slider

    private lateinit var appState: AppState

    private var brightnessTimeline: Timeline? = null
    private var contrastTimeline: Timeline? = null
    private var hueTimeline: Timeline? = null
    private var saturationTimeline: Timeline? = null
    private var lightnessTimeline: Timeline? = null
    private var yTimeline: Timeline? = null
    private var uTimeline: Timeline? = null
    private var vTimeline: Timeline? = null
    
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

        hueSlider.valueProperty().addListener { _, _, newValue ->
            val hue = newValue.toInt()
            hueValueLabel.text = "Matiz: %d".format(hue)

            hueTimeline?.stop()
            hueTimeline = Timeline(KeyFrame(Duration.millis(200.0), {
                appState.adjustHue(hue)
            }))
            hueTimeline?.play()
        }

        saturationSlider.valueProperty().addListener { _, _, newValue ->
            val saturation = newValue.toFloat() / 100.0f
            saturationValueLabel.text = "Saturación: %.2f".format(saturation)

            saturationTimeline?.stop()
            saturationTimeline = Timeline(KeyFrame(Duration.millis(200.0), {
                appState.adjustSaturation(saturation)
            }))
            saturationTimeline?.play()
        }

        lightnessSlider.valueProperty().addListener { _, _, newValue ->
            val lightness = newValue.toFloat() / 100.0f
            lightnessValueLabel.text = "Luminosidad: %.2f".format(lightness)

            lightnessTimeline?.stop()
            lightnessTimeline = Timeline(KeyFrame(Duration.millis(200.0), {
                appState.adjustLightness(lightness)
            }))
            lightnessTimeline?.play()
        }

        ySlider.valueProperty().addListener { _, _, newValue ->
            val yFactor = newValue.toFloat() / 100.0f
            yValueLabel.text = "Luma (Y): %.2f".format(yFactor)

            yTimeline?.stop()
            yTimeline = Timeline(KeyFrame(Duration.millis(200.0), {
                appState.adjustY(yFactor)
            }))
            yTimeline?.play()
        }

        uSlider.valueProperty().addListener { _, _, newValue ->
            val uFactor = newValue.toFloat() / 100.0f
            uValueLabel.text = "Croma (U): %.2f".format(uFactor)

            uTimeline?.stop()
            uTimeline = Timeline(KeyFrame(Duration.millis(200.0), {
                appState.adjustU(uFactor)
            }))
            uTimeline?.play()
        }

        vSlider.valueProperty().addListener { _, _, newValue ->
            val vFactor = newValue.toFloat() / 100.0f
            vValueLabel.text = "Croma (V): %.2f".format(vFactor)

            vTimeline?.stop()
            vTimeline = Timeline(KeyFrame(Duration.millis(200.0), {
                appState.adjustV(vFactor)
            }))
            vTimeline?.play()
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
            if (!hueSlider.isPressed) {
                hueSlider.value = context.hueFactor.toDouble()
                hueValueLabel.text = "Matiz: %d".format(context.hueFactor)
            }
            if (!saturationSlider.isPressed) {
                saturationSlider.value = context.saturationFactor * 100.0
                saturationValueLabel.text = "Saturación: %.2f".format(context.saturationFactor)
            }
            if (!lightnessSlider.isPressed) {
                lightnessSlider.value = context.lightnessFactor * 100.0
                lightnessValueLabel.text = "Luminosidad: %.2f".format(context.lightnessFactor)
            }
            if (!ySlider.isPressed) {
                ySlider.value = context.yFactor * 100.0
                yValueLabel.text = "Luma (Y): %.2f".format(context.yFactor)
            }
            if (!uSlider.isPressed) {
                uSlider.value = context.uFactor * 100.0
                uValueLabel.text = "Croma (U): %.2f".format(context.uFactor)
            }
            if (!vSlider.isPressed) {
                vSlider.value = context.vFactor * 100.0
                vValueLabel.text = "Croma (V): %.2f".format(context.vFactor)
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
