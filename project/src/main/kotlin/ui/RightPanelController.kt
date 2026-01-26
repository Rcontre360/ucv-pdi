package org.pdi.ui

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.stage.Modality
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.core.Kernel
import org.pdi.core.KernelType
import org.pdi.core.kernels.*
import org.pdi.ui.BorderDetectionType
import org.pdi.ui.panels.KernelMatrixPanelController

enum class BorderDetectionType {
    SOBEL,
    ROBERTS,
    PREWITT
}

// right panel has the border detection algorithms and kernel related stuff
class RightPanelController {

    // From FiltersPanelController
    @FXML
    private lateinit var typeComboBox: ComboBox<KernelType>

    @FXML
    private lateinit var rowsField: TextField

    @FXML
    private lateinit var colsField: TextField

    @FXML
    private lateinit var profilingFactorAdjusterController: ValueAdjusterController

    // From OperationsPanelController
    @FXML
    private lateinit var operationsComboBox: ComboBox<BorderDetectionType>

    private lateinit var appState: AppState
    private lateinit var primaryStage: Stage
    private val laplacianProfilingKernel = LaplacianKernelProfiling()
    private var currentKernel: Kernel = CustomKernel(3, 3) // Initialize with a default kernel

    fun setAppState(appState: AppState, primaryStage: Stage) {
        this.appState = appState
        this.primaryStage = primaryStage

        // FiltersPanelController logic
        typeComboBox.items.addAll(KernelType.values().toList())
        typeComboBox.selectionModel.selectFirst()

        rowsField.text = "3"
        colsField.text = "3"

        // Add listeners to rowsField and colsField to update currentKernel
        rowsField.textProperty().addListener { _, _, _ -> updateCurrentKernel() }
        colsField.textProperty().addListener { _, _, _ -> updateCurrentKernel() }

        profilingFactorAdjusterController.setup(1f, 1f, 10f, 1f) { newValue ->
            laplacianProfilingKernel.updateFactor(newValue.toInt())
        }

        typeComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            updateCurrentKernel() // Update currentKernel when type changes
        }

        // OperationsPanelController logic
        operationsComboBox.items.addAll(BorderDetectionType.values().toList())
        operationsComboBox.selectionModel.selectFirst()

        updateCurrentKernel()
    }

    @FXML
    fun showKernel() {
        val loader = FXMLLoader(javaClass.getResource("/panels/KernelMatrixPanel.fxml"))
        val root = loader.load<Parent>()
        val kernelMatrixPanelController: KernelMatrixPanelController = loader.getController()
        kernelMatrixPanelController.setKernel(currentKernel)

        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.initOwner(primaryStage)
        stage.title = "Kernel Matrix"
        stage.scene = Scene(root)
        stage.show()
    }

    @FXML
    fun applyFilter() {
        appState.applyConvolution(currentKernel)
    }

    private fun updateCurrentKernel() {
        val rows = rowsField.text.toIntOrNull() ?: 3
        val cols = colsField.text.toIntOrNull() ?: 3
        val selected = typeComboBox.selectionModel.selectedItem ?: KernelType.CUSTOM // Default if nothing selected

        currentKernel = createKernelInstance(rows, cols, selected)
        val (customRows, customCols) = currentKernel.isCustomizable()
        rowsField.isEditable = customRows
        colsField.isEditable = customCols
    }

    private fun createKernelInstance(rows: Int, cols: Int, selectedType: KernelType): Kernel {
        val kernel = when (selectedType) {
            KernelType.CUSTOM -> CustomKernel(rows, cols)
            KernelType.AVERAGE -> AverageKernel(rows, cols)
            KernelType.MEDIAN -> MedianKernel(rows, cols)
            KernelType.GAUSSIAN -> GaussianKernel(rows, cols)
            KernelType.LAPLACIAN -> LaplacianKernel(rows)
            KernelType.LAPLACIAN_PROFILING -> {
                laplacianProfilingKernel.rows = rows
                laplacianProfilingKernel.cols = cols
                laplacianProfilingKernel.generateKernel()
                laplacianProfilingKernel
            }

            KernelType.SOBEL_X -> SobelXKernel(rows)
            KernelType.SOBEL_Y -> SobelYKernel(rows)
            KernelType.ROBERTS_X -> RobertsXKernel()
            KernelType.ROBERTS_Y -> RobertsYKernel()
            KernelType.PREWITT_X -> PrewittXKernel()
            KernelType.PREWITT_Y -> PrewittYKernel()
            KernelType.ERODE -> ErodeKernel(rows, cols)
            KernelType.DILATE -> DilateKernel(rows, cols)
        }
        kernel.generateKernel()
        return kernel
    }

    @FXML
    fun applyOperation() {
        val selectedOperation = operationsComboBox.selectionModel.selectedItem as BorderDetectionType
        val (kernelX, kernelY) = when (selectedOperation) {
            BorderDetectionType.SOBEL -> Pair(SobelXKernel(3), SobelYKernel(3))
            BorderDetectionType.ROBERTS -> Pair(RobertsXKernel(), RobertsYKernel())
            BorderDetectionType.PREWITT -> Pair(PrewittXKernel(), PrewittYKernel())
        }
        appState.applyBorderOperator(kernelX, kernelY)
    }
}