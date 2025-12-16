package org.pdi.ui

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import org.pdi.core.AppState
import org.pdi.ui.BorderDetectionType
import org.pdi.core.kernels.*

class OperationsPanelController {

    @FXML
    private lateinit var operationsComboBox: ComboBox<BorderDetectionType>

    private lateinit var appState: AppState

    fun setAppState(appState: AppState) {
        this.appState = appState
        operationsComboBox.items.addAll(BorderDetectionType.values())
        operationsComboBox.selectionModel.selectFirst()
    }

    @FXML
    fun applyOperation() {
        val selectedOperation = operationsComboBox.selectionModel.selectedItem
        val (kernelX, kernelY) = when (selectedOperation) {
            BorderDetectionType.SOBEL -> Pair(SobelXKernel(3), SobelYKernel(3))
            BorderDetectionType.ROBERTS -> Pair(RobertsXKernel(), RobertsYKernel())
            BorderDetectionType.PREWITT -> Pair(PrewittXKernel(), PrewittYKernel())
            else -> Pair(SobelXKernel(3), SobelYKernel(3))
        }
        appState.applyBorderOperator(kernelX, kernelY)
    }
}
