package org.pdi.ui

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.pdi.core.AppState
import org.pdi.core.Kernel
import org.pdi.core.KernelType
import org.pdi.core.kernels.*

class FiltersPanelController {

    @FXML
    private lateinit var typeComboBox: ComboBox<KernelType>

    @FXML
    private lateinit var rowsField: TextField

    @FXML
    private lateinit var colsField: TextField

    @FXML
    private lateinit var profilingFactorAdjusterController: ValueAdjusterController

    private lateinit var appState: AppState
    private val laplacianProfilingKernel = LaplacianKernelProfiling()

    fun setAppState(appState: AppState) {
        this.appState = appState
        typeComboBox.items.addAll(KernelType.values())
        typeComboBox.selectionModel.selectFirst()

        profilingFactorAdjusterController.setup(1f, 1f, 10f, 1f) { newValue ->
            laplacianProfilingKernel.updateFactor(newValue.toInt())
            appState.applyConvolution(laplacianProfilingKernel)
        }

        typeComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            val kernel = createKernel()
            val (customRows, customCols) = kernel.isCustomizable()
            rowsField.isEditable = customRows
            colsField.isEditable = customCols
        }
    }

    @FXML
    fun showKernel() {
        val kernel = createKernel()
        val loader = FXMLLoader(javaClass.getResource("/KernelMatrixPanel.fxml"))
        val root = loader.load<Parent>()
        val kernelMatrixPanelController: KernelMatrixPanelController = loader.getController()
        kernelMatrixPanelController.setKernel(kernel)

        val stage = Stage()
        stage.title = "Kernel Matrix"
        stage.scene = Scene(root)
        stage.show()
    }

    @FXML
    fun applyFilter() {
        appState.applyConvolution(createKernel())
    }

    private fun createKernel(): Kernel {
        val rows = rowsField.text.toIntOrNull() ?: 3
        val cols = colsField.text.toIntOrNull() ?: 3
        val selected = typeComboBox.selectionModel.selectedItem
        val kernel = when (selected) {
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
            else -> CustomKernel(rows, cols)
        }
        kernel.generateKernel()
        return kernel
    }
}
