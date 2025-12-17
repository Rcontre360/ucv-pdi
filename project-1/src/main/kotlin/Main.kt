package org.pdi

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import org.pdi.core.AppState

// app start. a template I found out there
class Main : Application() {
    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.getResource("/main.fxml"))
        val mainController = MainController(primaryStage) // Pass primaryStage to MainController
        loader.setController(mainController)
        val root = loader.load<Parent>()
        primaryStage.title = "Image Viewer"
        primaryStage.minWidth = 1040.0
        primaryStage.minHeight = 600.0
        val scene = Scene(root, 1040.0, 600.0)
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java, *args)
}

