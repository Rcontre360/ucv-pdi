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
import org.opencv.core.Point
import org.pdi.core.image.toBufferedImage
import javafx.embed.swing.SwingFXUtils
import org.opencv.core.Mat
import org.pdi.core.rg.*

class RegionGrowingPanelController {

    @FXML private lateinit var imagePane: Pane
    @FXML private lateinit var regionGrowingImageView: ImageView
    @FXML private lateinit var modeComboBox: ComboBox<String>
    @FXML private lateinit var connectivityComboBox: ComboBox<String>
    @FXML private lateinit var maxAbsDiffTextField: TextField
    @FXML private lateinit var applyButton: Button

    private lateinit var appState: AppState
    private lateinit var image: Mat
    private val seedPoints = mutableListOf<Point>()

    fun initialize(appState: AppState) {
        this.appState = appState
        val currentMat = appState.getImage()
        if (currentMat == null) {
            close()
            return
        }
        this.image = currentMat
        regionGrowingImageView.image = SwingFXUtils.toFXImage(this.image.toBufferedImage(), null)

        setupComboBoxes()
        imagePane.setOnMouseClicked(this::handleImageClick)
    }

    private fun setupComboBoxes() {
        modeComboBox.items = FXCollections.observableArrayList("Fixed Range", "Floating Range")
        modeComboBox.selectionModel.selectFirst()

        connectivityComboBox.items = FXCollections.observableArrayList("4-connectivity", "8-connectivity")
        connectivityComboBox.selectionModel.select("8-connectivity")
    }

    private fun handleImageClick(event: MouseEvent) {
        val bounds = regionGrowingImageView.boundsInParent
        if (event.x !in bounds.minX..bounds.maxX || event.y !in bounds.minY..bounds.maxY) return

        val scale = minOf(bounds.width / image.width(), bounds.height / image.height())
        val offsetX = (bounds.width - (image.width() * scale)) / 2 + bounds.minX
        val offsetY = (bounds.height - (image.height() * scale)) / 2 + bounds.minY

        val imageX = ((event.x - offsetX) / scale).toInt()
        val imageY = ((event.y - offsetY) / scale).toInt()

        if (imageX in 0 until image.width() && imageY in 0 until image.height()) {
            seedPoints.add(Point(imageX.toDouble(), imageY.toDouble()))
            val circle = Circle((imageX * scale) + offsetX, (imageY * scale) + offsetY, 3.0, Color.RED)
            imagePane.children.add(circle)
        }
    }

    @FXML fun cancel() = close()

    @FXML fun apply() {
        val threshold = maxAbsDiffTextField.text.toDoubleOrNull() ?: 20.0
        val conn = if (connectivityComboBox.value == "4-connectivity") 4 else 8
        val isFixed = modeComboBox.value == "Fixed Range"

        if (seedPoints.isNotEmpty()) {
            val algorithms: List<RegionGrowing> = seedPoints.map { point ->
                if (isFixed) {
                    FixedRegionGrowing(point, threshold, conn)
                } else {
                    FloatingRegionGrowing(point, threshold, conn)
                }
            }

            appState.applyRegionGrowing(algorithms)
            close()
        }
    }

    private fun close() = (applyButton.scene.window as Stage).close()
}