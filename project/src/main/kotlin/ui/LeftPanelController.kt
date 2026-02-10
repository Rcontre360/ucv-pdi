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

class LeftPanelController {

    @FXML private lateinit var brightnessValueLabel: Label
    @FXML private lateinit var brightnessSlider: Slider

    @FXML private lateinit var contrastValueLabel: Label
    @FXML private lateinit var contrastSlider: Slider

    @FXML private lateinit var infoPanelController: InfoPanelController

    @FXML private lateinit var colorPicker: ColorPicker

    @FXML private lateinit var hueValueLabel: Label
    @FXML private lateinit var hueSlider: Slider

    @FXML private lateinit var saturationValueLabel: Label
    @FXML private lateinit var saturationSlider: Slider

    @FXML private lateinit var lightnessValueLabel: Label
    @FXML private lateinit var lightnessSlider: Slider

    @FXML private lateinit var tempValueLabel: Label
    @FXML private lateinit var tempSlider: Slider

    private lateinit var appState: AppState

    private var brightnessTimeline: Timeline? = null
    private var contrastTimeline: Timeline? = null
    private var hueTimeline: Timeline? = null
    private var saturationTimeline: Timeline? = null
    private var lightnessTimeline: Timeline? = null
    private var tempTimeline: Timeline? = null

    fun setAppState(appState: AppState) {
        this.appState = appState
        infoPanelController.setAppState(appState)

        brightnessSlider.valueProperty().addListener { _, _, newValue ->
            val brightness = newValue.toFloat() / 100.0f
            brightnessValueLabel.text = "Brightness: %.2f".format(brightness)

            if (brightnessSlider.isValueChanging || brightnessSlider.isPressed) {
                debounce(brightnessTimeline, { appState.setBrightness(brightness) }) { brightnessTimeline = it }
            }
        }

        contrastSlider.valueProperty().addListener { _, _, newValue ->
            val contrast = newValue.toFloat() / 100.0f
            contrastValueLabel.text = "Contrast: %.2f".format(contrast)

            if (contrastSlider.isValueChanging || contrastSlider.isPressed) {
                debounce(contrastTimeline, { appState.setContrast(contrast) }) { contrastTimeline = it }
            }
        }

        hueSlider.valueProperty().addListener { _, _, newValue ->
            val hue = newValue.toInt()
            hueValueLabel.text = "Matiz: %d".format(hue)

            if (hueSlider.isValueChanging || hueSlider.isPressed) {
                debounce(hueTimeline, { appState.adjustHue(hue) }) { hueTimeline = it }
            }
        }

        saturationSlider.valueProperty().addListener { _, _, newValue ->
            val saturation = newValue.toFloat() / 100.0f
            saturationValueLabel.text = "Saturación: %.2f".format(saturation)

            if (saturationSlider.isValueChanging || saturationSlider.isPressed) {
                debounce(saturationTimeline, { appState.adjustSaturation(saturation) }) { saturationTimeline = it }
            }
        }

        lightnessSlider.valueProperty().addListener { _, _, newValue ->
            val lightness = newValue.toFloat() / 100.0f
            lightnessValueLabel.text = "Luminosidad: %.2f".format(lightness)

            if (lightnessSlider.isValueChanging || lightnessSlider.isPressed) {
                debounce(lightnessTimeline, { appState.adjustLightness(lightness) }) { lightnessTimeline = it }
            }
        }

        tempSlider.valueProperty().addListener { _, _, newValue ->
            val temp = newValue.toFloat()
            tempValueLabel.text = "Temp: %.0f".format(temp)

            if (tempSlider.isValueChanging || tempSlider.isPressed) {
                debounce(tempTimeline, { appState.adjustTemperature(temp) }) { tempTimeline = it }
            }
        }

        appState.addContextListener { context ->
            if (!brightnessSlider.isPressed && !brightnessSlider.isValueChanging) {
                brightnessSlider.value = context.brightness * 100.0
                brightnessValueLabel.text = "Brightness: %.2f".format(context.brightness)
            }
            if (!contrastSlider.isPressed && !contrastSlider.isValueChanging) {
                contrastSlider.value = context.contrast * 100.0
                contrastValueLabel.text = "Contrast: %.2f".format(context.contrast)
            }
            if (!hueSlider.isPressed && !hueSlider.isValueChanging) {
                hueSlider.value = context.hueFactor.toDouble()
                hueValueLabel.text = "Matiz: %d".format(context.hueFactor)
            }
            if (!saturationSlider.isPressed && !saturationSlider.isValueChanging) {
                saturationSlider.value = context.saturationFactor * 100.0
                saturationValueLabel.text = "Saturación: %.2f".format(context.saturationFactor)
            }
            if (!lightnessSlider.isPressed && !lightnessSlider.isValueChanging) {
                lightnessSlider.value = context.lightnessFactor * 100.0
                lightnessValueLabel.text = "Luminosidad: %.2f".format(context.lightnessFactor)
            }
            if (!tempSlider.isPressed && !tempSlider.isValueChanging) {
                context.currentImage?.let {
                    val currentTemp = it.getTemperature()
                    tempSlider.value = currentTemp.toDouble()
                    tempValueLabel.text = "Temp: %.0f".format(currentTemp)
                }
            }
        }
    }

    private fun debounce(timeline: Timeline?, action: () -> Unit, updateTimeline: (Timeline) -> Unit) {
        timeline?.stop()
        val newTimeline = Timeline(KeyFrame(Duration.millis(200.0), { action() }))
        updateTimeline(newTimeline)
        newTimeline.play()
    }

    @FXML
    fun applyGrayscale() {
        val color = colorPicker.value
        val awtColor = java.awt.Color(color.red.toFloat(), color.green.toFloat(), color.blue.toFloat())
        appState.applyGrayscale(awtColor)
    }

    @FXML
    fun applyNegative() = appState.applyNegative()
}