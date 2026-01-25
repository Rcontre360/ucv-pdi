package org.pdi.ui.panels

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.core.Image
import org.pdi.io.toWritableImage
import org.opencv.core.Point

class RegionGrowingPanelController {

    @FXML
    private lateinit var imagePane: Pane

    @FXML
    private lateinit var regionGrowingImageView: ImageView

    @FXML
    private lateinit var modeComboBox: ComboBox<String>

    @FXML
    private lateinit var connectivityComboBox: ComboBox<String>

    @FXML
    private lateinit var maxAbsDiffTextField: TextField

    @FXML
    private lateinit var cancelButton: Button

    @FXML
    private lateinit var applyButton: Button

    private lateinit var appState: AppState
    private lateinit var image: Image
    private val seedPoints = mutableListOf<Point>()

    fun initialize(appState: AppState, image: Image) {
        this.appState = appState
        this.image = image
        regionGrowingImageView.image = image.image.toWritableImage()
        regionGrowingImageView.isPreserveRatio = true

        // Mode ComboBox setup
        modeComboBox.items.addAll(FXCollections.observableArrayList("Fixed Range", "Floating Range"))
        modeComboBox.selectionModel.select("Fixed Range")
        // Disable "Floating Range" item. A bit tricky with ComboBox, but we ensure it's not selectable
        modeComboBox.setCellFactory { _ ->
            object : javafx.scene.control.ListCell<String>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = item
                    if (item == "Floating Range") {
                        isDisable = true // This disables the item
                    }
                }
            }
        }
        modeComboBox.valueProperty().addListener { _, _, newValue ->
            if (newValue == "Floating Range") {
                modeComboBox.selectionModel.select("Fixed Range") // Force selection back to Fixed if attempted
            }
        }


        // Connectivity ComboBox setup
        connectivityComboBox.items.addAll(FXCollections.observableArrayList("4-connectivity", "8-connectivity"))
        connectivityComboBox.selectionModel.select("8-connectivity")

        imagePane.setOnMouseClicked(this::handleImageClick)
    }

    private fun handleImageClick(event: MouseEvent) {
        val imageViewBounds = regionGrowingImageView.boundsInParent
        val imageSize = image.metadata
        
        // Check if the click is within the image view bounds
        if (event.x < imageViewBounds.minX || event.x > imageViewBounds.maxX || event.y < imageViewBounds.minY || event.y > imageViewBounds.maxY) {
            return
        }

        // Calculate scale factor
        val scaleX = imageViewBounds.width / imageSize.width
        val scaleY = imageViewBounds.height / imageSize.height
        val scale = minOf(scaleX, scaleY)

        // Calculate the actual displayed image dimensions
        val displayWidth = imageSize.width * scale
        val displayHeight = imageSize.height * scale

        // Calculate the offset (blank space) around the image
        val offsetX = (imageViewBounds.width - displayWidth) / 2
        val offsetY = (imageViewBounds.height - displayHeight) / 2

        // Translate click coordinates to image coordinates
        val imageX = ((event.x - imageViewBounds.minX - offsetX) / scale).toInt()
        val imageY = ((event.y - imageViewBounds.minY - offsetY) / scale).toInt()

        if (imageX >= 0 && imageX < imageSize.width && imageY >= 0 && imageY < imageSize.height) {
            seedPoints.add(Point(imageX.toDouble(), imageY.toDouble()))

            // Draw a circle on the Pane
            val circleRadius = imageSize.width * 0.01
            val viewX = (imageX * scale) + imageViewBounds.minX + offsetX
            val viewY = (imageY * scale) + imageViewBounds.minY + offsetY
            
            val circle = Circle(viewX, viewY, circleRadius, Color.RED)
            imagePane.children.add(circle)
        }
    }

    @FXML
    fun cancel() {
        val stage = cancelButton.scene.window as Stage
        stage.close()
    }

    @FXML
    fun apply() {
        val maxDiff = maxAbsDiffTextField.text.toIntOrNull() ?: 20
        val connectivity = if (connectivityComboBox.selectionModel.selectedItem == "4-connectivity") 4 else 8

        if (seedPoints.isNotEmpty()) {
            appState.applyRegionGrowing(seedPoints, maxDiff, connectivity)
        }

        val stage = applyButton.scene.window as Stage
        stage.close()
    }
}
