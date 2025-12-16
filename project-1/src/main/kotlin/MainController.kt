package org.pdi

import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.image.ImageView
import org.pdi.core.AppState
import org.pdi.ui.BottomPanelController
import org.pdi.ui.FiltersPanelController
import org.pdi.ui.LeftPanelController
import org.pdi.ui.OperationsPanelController
import org.pdi.ui.TopPanelController

class MainController {

    @FXML
    private lateinit var topPanelController: TopPanelController

    @FXML
    private lateinit var leftPanelController: LeftPanelController

    @FXML
    private lateinit var filtersPanelController: FiltersPanelController

    @FXML
    private lateinit var operationsPanelController: OperationsPanelController

    @FXML
    private lateinit var bottomPanelController: BottomPanelController

    @FXML
    private lateinit var imageView: ImageView

    private val appState = AppState()

    @FXML
    fun initialize() {
        topPanelController.setAppState(appState)
        leftPanelController.setAppState(appState)
        filtersPanelController.setAppState(appState)
        operationsPanelController.setAppState(appState)
        bottomPanelController.setAppState(appState)

        appState.addContextListener { stateContext ->
            if (stateContext.currentImage != null) {
                val fxImage = SwingFXUtils.toFXImage(stateContext.currentImage.image, null)
                imageView.image = fxImage
            } else {
                imageView.image = null
            }
        }
    }
}
