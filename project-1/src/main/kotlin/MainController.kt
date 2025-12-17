package org.pdi

import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.image.ImageView
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.ui.BottomPanelController
import org.pdi.ui.LeftPanelController
import org.pdi.ui.RightPanelController
import org.pdi.ui.TopPanelController


// main controller. Just has a context listener and the 4 panels (top, bottom, left, right)
class MainController(private val primaryStage: Stage) {

    @FXML
    private lateinit var topPanelController: TopPanelController

    @FXML
    private lateinit var leftPanelController: LeftPanelController

    @FXML
    private lateinit var rightPanelController: RightPanelController

    @FXML
    private lateinit var bottomPanelController: BottomPanelController

    @FXML
    private lateinit var imageView: ImageView

    private val appState = AppState()

    @FXML
    fun initialize() {
        topPanelController.setAppState(appState, primaryStage) 
        leftPanelController.setAppState(appState)
        rightPanelController.setAppState(appState, primaryStage)
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
