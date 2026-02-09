package org.pdi.ui.panels

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.core.image.Image
import org.opencv.core.Point
import org.pdi.core.image.toBufferedImage
import javafx.embed.swing.SwingFXUtils

class RegionGrowingPanelController {

    @FXML private lateinit var imagePane: Pane
    @FXML private lateinit var regionGrowingImageView: ImageView
    @FXML private lateinit var modeComboBox: ComboBox<String>
    @FXML private lateinit var connectivityComboBox: ComboBox<String>
    @FXML private lateinit var maxAbsDiffTextField: TextField
    @FXML private lateinit var cancelButton: Button
    @FXML private lateinit var applyButton: Button

    private lateinit var appState: AppState
    private lateinit var image: Image
    private val seedPoints = mutableListOf<Point>()

    fun initialize(appState: AppState, image: Image) {
        this.appState = appState
        this.image = image
        regionGrowingImageView.image = SwingFXUtils.toFXImage(image.image.toBufferedImage(), null)

        modeComboBox.items = FXCollections.observableArrayList("Fixed Range", "Floating Range")
        modeComboBox.selectionModel.select("Fixed Range")

        modeComboBox.setCellFactory { _ ->
            object : ListCell<String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = item
                    isDisable = item == "Floating Range"
                }
            }
        }

        modeComboBox.valueProperty().addListener { _, _, newValue ->
            if (newValue == "Floating Range") modeComboBox.selectionModel.select("Fixed Range")
        }

        connectivityComboBox.items = FXCollections.observableArrayList("4-connectivity", "8-connectivity")
        connectivityComboBox.selectionModel.select("8-connectivity")

        imagePane.setOnMouseClicked(this::handleImageClick)
    }

    private fun handleImageClick(event: MouseEvent) {
        val bounds = regionGrowingImageView.boundsInParent
        if (event.x !in bounds.minX..bounds.maxX || event.y !in bounds.minY..bounds.maxY) return

        val meta = image.metadata
        val scale = minOf(bounds.width / meta.width, bounds.height / meta.height)
        val offsetX = (bounds.width - (meta.width * scale)) / 2
        val offsetY = (bounds.height - (meta.height * scale)) / 2

        val imageX = ((event.x - bounds.minX - offsetX) / scale).toInt()
        val imageY = ((event.y - bounds.minY - offsetY) / scale).toInt()

        if (imageX in 0 until meta.width && imageY in 0 until meta.height) {
            seedPoints.add(Point(imageX.toDouble(), imageY.toDouble()))

            val viewX = (imageX * scale) + bounds.minX + offsetX
            val viewY = (imageY * scale) + bounds.minY + offsetY
            imagePane.children.add(Circle(viewX, viewY, meta.width * 0.01, Color.RED))
        }
    }

    @FXML fun cancel() = (cancelButton.scene.window as Stage).close()

    @FXML fun apply() {
        val maxDiff = maxAbsDiffTextField.text.toIntOrNull() ?: 20
        val conn = if (connectivityComboBox.value == "4-connectivity") 4 else 8

        if (seedPoints.isNotEmpty()) appState.applyRegionGrowing(seedPoints, maxDiff, conn)
        (applyButton.scene.window as Stage).close()
    }
}