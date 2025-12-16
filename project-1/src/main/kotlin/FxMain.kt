package org.pdi

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class FxMain : Application() {
    override fun start(primaryStage: Stage) {
        primaryStage.title = "Hello World!"
        val label = Label("Hello World!")
        val root = StackPane(label)
        val scene = Scene(root, 300.0, 200.0)
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(FxMain::class.java, *args)
}
