package org.pdi

import javafx.fxml.FXML
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.stage.Stage
import javafx.event.EventHandler // Import EventHandler

import org.pdi.core.AppState
import org.pdi.core.image.toBufferedImage
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
    private var lastMouseX: Double = 0.0
    private var lastMouseY: Double = 0.0

    @FXML
    fun initialize() {
        topPanelController.setAppState(appState, primaryStage)
        leftPanelController.setAppState(appState)
        rightPanelController.setAppState(appState, primaryStage)
        bottomPanelController.setAppState(appState)

        appState.addContextListener { stateContext ->
            if (stateContext.currentImage != null) {
                val fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(stateContext.currentImage.image.toBufferedImage(), null)
                imageView.image = fxImage
            } else {
                imageView.image = null
            }

            // Handle panning mode activation/deactivation
            if (stateContext.isPanningMode) {
                imageView.onMousePressed = EventHandler { event: MouseEvent -> handleMousePressed(event) }
                imageView.onMouseDragged = EventHandler { event: MouseEvent -> handleMouseDragged(event) }
                imageView.onMouseReleased = EventHandler { event: MouseEvent -> handleMouseReleased(event) }
            } else {
                imageView.onMousePressed = null
                imageView.onMouseDragged = null
                imageView.onMouseReleased = null
            }
        }
    }

    private fun handleMousePressed(event: MouseEvent) {
        if (appState.context.isPanningMode) {
            lastMouseX = event.sceneX
            lastMouseY = event.sceneY
            event.consume()
        }
    }

    private fun handleMouseDragged(event: MouseEvent) {
        if (appState.context.isPanningMode) {
            val deltaX = event.sceneX - lastMouseX
            val deltaY = event.sceneY - lastMouseY

            appState.applyTranslation(deltaX.toInt(), deltaY.toInt())

            lastMouseX = event.sceneX
            lastMouseY = event.sceneY
            event.consume()
        }
    }

    private fun handleMouseReleased(event: MouseEvent) {
        if (appState.context.isPanningMode) {
            // No specific action needed on release for continuous panning
            event.consume()
        }
    }
}
