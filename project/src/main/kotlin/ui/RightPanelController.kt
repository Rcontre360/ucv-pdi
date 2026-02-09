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
import org.pdi.core.kernels.*
import org.pdi.ui.panels.KernelMatrixPanelController

enum class BorderDetectionType {
    SOBEL, ROBERTS, PREWITT
}

// Representamos las operaciones compuestas localmente para la UI
enum class MorphOp {
    ERODE, DILATE, OPEN, CLOSE
}

class RightPanelController {

    @FXML private lateinit var typeComboBox: ComboBox<KernelType>
    @FXML private lateinit var rowsField: TextField
    @FXML private lateinit var colsField: TextField
    @FXML private lateinit var profilingFactorAdjusterController: ValueAdjusterController

    @FXML private lateinit var bordersComboBox: ComboBox<BorderDetectionType>

    // Usamos un String o un enum local para incluir Open y Close sin tocar KernelType
    @FXML private lateinit var morphComboBox: ComboBox<MorphOp>
    @FXML private lateinit var morphRowsField: TextField
    @FXML private lateinit var morphColsField: TextField

    private lateinit var appState: AppState
    private lateinit var primaryStage: Stage
    private val laplacianProfiling = LaplacianKernelProfiling()
    private val kernelManager = KernelConfigManager(laplacianProfiling)
    private var currentKernel: Kernel = CustomKernel(3, 3)

    fun setAppState(appState: AppState, primaryStage: Stage) {
        this.appState = appState
        this.primaryStage = primaryStage

        setupKernelSection()
        setupBordersSection()
        setupMorphologySection()

        updateCurrentKernel()
    }

    private fun setupKernelSection() {
        typeComboBox.items.addAll(KernelType.entries.filter {
            it != KernelType.ERODE && it != KernelType.DILATE
        })
        typeComboBox.selectionModel.select(KernelType.CUSTOM)

        val updateListener = { _: Any?, _: Any?, _: Any? -> updateCurrentKernel() }
        rowsField.textProperty().addListener(updateListener)
        colsField.textProperty().addListener(updateListener)
        typeComboBox.valueProperty().addListener(updateListener)

        profilingFactorAdjusterController.setup(1f, 1f, 10f, 1f) { newValue ->
            laplacianProfiling.updateFactor(newValue.toInt())
            updateCurrentKernel()
        }
    }

    private fun setupBordersSection() {
        bordersComboBox.items.addAll(BorderDetectionType.entries.toList())
        bordersComboBox.selectionModel.selectFirst()
    }

    private fun setupMorphologySection() {
        morphComboBox.items.addAll(MorphOp.entries.toList())
        morphComboBox.selectionModel.selectFirst()
    }

    private fun updateCurrentKernel() {
        val r = rowsField.text.toIntOrNull() ?: 3
        val c = colsField.text.toIntOrNull() ?: 3
        val selected = typeComboBox.value ?: KernelType.CUSTOM

        currentKernel = kernelManager.createInstance(r, c, selected)

        val (customR, customC) = currentKernel.isCustomizable()
        rowsField.isEditable = customR
        colsField.isEditable = customC
    }

    @FXML fun showKernel() = openKernelMatrixWindow(currentKernel, false)

    @FXML fun applyFilter() = appState.applyConvolution(currentKernel)

    @FXML
    fun applyBorders() {
        val selected = bordersComboBox.value ?: BorderDetectionType.SOBEL
        val (kX, kY) = when (selected) {
            BorderDetectionType.SOBEL -> SobelXKernel(3) to SobelYKernel(3)
            BorderDetectionType.ROBERTS -> RobertsXKernel() to RobertsYKernel()
            else -> PrewittXKernel() to PrewittYKernel()
        }
        appState.applyBorderOperator(kX, kY)
    }

    @FXML
    fun showMorphologyEditor() {
        val r = morphRowsField.text.toIntOrNull() ?: 3
        val c = morphColsField.text.toIntOrNull() ?: 3

        // Mapeamos la selección de la UI a un KernelType básico para el editor
        val type = when (morphComboBox.value) {
            MorphOp.DILATE, MorphOp.CLOSE -> KernelType.DILATE
            else -> KernelType.ERODE
        }

        val morphKernel = kernelManager.createInstance(r, c, type)
        openKernelMatrixWindow(morphKernel, true)
    }

    @FXML
    fun applyMorphology() {
        val r = morphRowsField.text.toIntOrNull() ?: 3
        val c = morphColsField.text.toIntOrNull() ?: 3
        val op = morphComboBox.value ?: MorphOp.ERODE

        // Preparamos las instancias de kernel (ambas con las mismas dimensiones)
        val erodeKernel = kernelManager.createInstance(r, c, KernelType.ERODE)
        val dilateKernel = kernelManager.createInstance(r, c, KernelType.DILATE)

        when (op) {
            MorphOp.ERODE -> appState.applyConvolution(erodeKernel)
            MorphOp.DILATE -> appState.applyConvolution(dilateKernel)
            MorphOp.OPEN -> {
                // Apertura = Erosion -> Dilatacion
                appState.applyConvolution(erodeKernel)
                appState.applyConvolution(dilateKernel)
            }
            MorphOp.CLOSE -> {
                // Cierre = Dilatacion -> Erosion
                appState.applyConvolution(dilateKernel)
                appState.applyConvolution(erodeKernel)
            }
        }
    }

    private fun openKernelMatrixWindow(kernel: Kernel, binaryOnly: Boolean) {
        val loader = FXMLLoader(javaClass.getResource("/panels/KernelMatrixPanel.fxml"))
        val root = loader.load<Parent>()
        val controller: KernelMatrixPanelController = loader.getController()

        controller.setKernel(kernel)

        Stage().apply {
            initModality(Modality.APPLICATION_MODAL)
            initOwner(primaryStage)
            title = if (binaryOnly) "Structuring Element (Binary)" else "Kernel Matrix"
            scene = Scene(root)
            show()
        }
    }
}