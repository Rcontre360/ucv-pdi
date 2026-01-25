package org.pdi.ui.panels

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import org.pdi.core.AppState
import org.pdi.io.toBufferedImage
import java.awt.Point
import kotlin.math.roundToInt


// line profile panel. Complicated to build since we had to show the image again to be able to select it
class LineProfilePanelController {

    @FXML
    private lateinit var channelComboBox: ComboBox<String>

    @FXML
    private lateinit var lineGraphCanvas: Canvas

    @FXML
    private lateinit var lineProfileImageView: javafx.scene.image.ImageView

    @FXML
    private lateinit var axisToggleGroup: ToggleGroup

    private lateinit var appState: AppState
    private var profileData: List<Pair<Int, Int>> = emptyList()

    fun setAppState(appState: AppState) {
        this.appState = appState
        channelComboBox.items.addAll(FXCollections.observableArrayList("R", "G", "B", "Gray"))
        channelComboBox.selectionModel.selectFirst()

        axisToggleGroup.selectedToggleProperty()?.addListener { _, _, _ ->
            drawGraph()
        }
        channelComboBox.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            drawGraph()
        }

        val fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(appState.getImage()?.toBufferedImage(), null)
        lineProfileImageView.image = fxImage

        // this is the listener to the user clicks on the image
        // for this to work the conatiner MUST be the exact same size of the image on the UI fxml definition
        lineProfileImageView.setOnMouseClicked { event ->
            val img = appState.getImage()
            if (img != null) {
                updateLineProfile(Point(event.x.toInt(), event.y.toInt()))
            }
        }
    }

    private fun updateLineProfile(point: Point) {
        val image = appState.context.currentImage
        if (image == null) {
            showAlert("No Image Loaded", "No image loaded.")
            return
        }

        val axis = axisToggleGroup.selectedToggle.userData as String
        val lineNumber = if (axis == "X") point.y else point.x
        val channel = channelComboBox.selectionModel.selectedItem[0]

        val data = appState.context.currentImage?.getLineProfile(axis[0], lineNumber, channel)

        if (data != null) {
            profileData = data
            drawGraph()
        } else {
            showAlert("Error", "Could not generate line profile.")
        }
    }

    private fun drawGraph() {
        val gc: GraphicsContext = lineGraphCanvas.graphicsContext2D
        gc.clearRect(0.0, 0.0, lineGraphCanvas.width, lineGraphCanvas.height)

        val width = lineGraphCanvas.width
        val height = lineGraphCanvas.height

        gc.stroke = Color.BLACK
        gc.strokeRect(0.0, 0.0, width, height)

        if (profileData.isEmpty()) {
            gc.fill = Color.LIGHTGRAY
            gc.font = javafx.scene.text.Font.font("SansSerif", 14.0)
            gc.fillText("No profile data available", 50.0, height / 2)
            return
        }

        val maxIndex = profileData.maxOf { it.first }
        val scaleX = width / maxIndex.toFloat()
        val scaleY = height / 255f // Max pixel value is 255

        gc.stroke = Color.BLUE
        gc.lineWidth = 1.0

        gc.beginPath()
        gc.moveTo(profileData[0].first * scaleX, height - profileData[0].second * scaleY)

        for (i in 1 until profileData.size) {
            val p = profileData[i]
            val x = p.first * scaleX
            val y = height - p.second * scaleY
            gc.lineTo(x, y)
        }
        gc.stroke()
    }

    private fun showAlert(title: String, message: String) {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
