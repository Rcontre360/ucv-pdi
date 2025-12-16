package org.pdi.ui

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import org.pdi.core.AppState
import org.pdi.ui.panels.InfoPanelController

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
    private lateinit var colorDisplay: Region

    @FXML
    private lateinit var infoPanelController: InfoPanelController

    private lateinit var appState: AppState
    private var selectedColor: Color = Color.WHITE

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

    @FXML
    fun selectColor() {
        // There is no direct equivalent of JColorChooser in JavaFX standard library.
        // A custom dialog or a third-party library would be needed.
        // For now, we will just cycle through a few colors.
        selectedColor = when (selectedColor) {
            Color.WHITE -> Color.RED
            Color.RED -> Color.GREEN
            Color.GREEN -> Color.BLUE
            else -> Color.WHITE
        }
        colorDisplay.style = "-fx-background-color: ${toHex(selectedColor)}; -fx-border-color: black;"
    }

    private fun toHex(color: Color): String {
        return "#${color.red.toString().substring(2, 4)}${color.green.toString().substring(2, 4)}${color.blue.toString().substring(2, 4)}"
    }
}
