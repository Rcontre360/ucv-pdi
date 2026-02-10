package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.Label
import org.pdi.core.AppState
import org.pdi.core.image.Metadata

// info panel, updates each time the app state is updated
class InfoPanelController {

    @FXML
    private lateinit var widthLabel: Label

    @FXML
    private lateinit var heightLabel: Label

    @FXML
    private lateinit var bppLabel: Label

    @FXML
    private lateinit var uniqueColorsLabel: Label

    @FXML
    private lateinit var formatLabel: Label

    private lateinit var appState: AppState

    fun setAppState(appState: AppState) {
        this.appState = appState
        appState.addContextListener { stateContext ->
            if (stateContext.currentImage != null) {
                updateMetadata(stateContext.currentImage.metadata)
            } else {
                updateMetadata(null)
            }
        }
    }

    private fun updateMetadata(metadata: Metadata?) {
        if (metadata != null) {
            widthLabel.text = "Width: ${metadata.width}"
            heightLabel.text = "Height: ${metadata.height}"
            bppLabel.text = "Bits Per Pixel: ${metadata.bitsPerPixel}"
            uniqueColorsLabel.text = "Unique Colors: ${metadata.uniqueColors}"
            formatLabel.text = "Format: ${metadata.format}"
        } else {
            widthLabel.text = "Width: "
            heightLabel.text = "Height: "
            bppLabel.text = "Bits Per Pixel: "
            uniqueColorsLabel.text = "Unique Colors: "
            formatLabel.text = "Format: "
        }
    }
}
