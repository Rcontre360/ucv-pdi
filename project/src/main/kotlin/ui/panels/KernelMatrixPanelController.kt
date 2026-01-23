package org.pdi.ui.panels

import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import org.pdi.core.Kernel

// kernel matrix panel. Can be updated by the user
class KernelMatrixPanelController {

    @FXML
    private lateinit var gridPane: GridPane

    private lateinit var kernel: Kernel

    fun setKernel(kernel: Kernel) {
        this.kernel = kernel
        populateGridPane()
    }

    private fun populateGridPane() {
        gridPane.children.clear()
        for (i in 0 until kernel.rows) {
            for (j in 0 until kernel.cols) {
                val field = TextField(kernel.kernel[i][j].toString()).apply {
                    prefWidth = 50.0
                    textProperty().addListener { _, _, newValue ->
                        val newValueFloat = newValue.toFloatOrNull()
                        if (newValueFloat != null) {
                            kernel.setKernelValue(i, j, newValueFloat)
                        }
                    }
                }
                gridPane.add(field, j, i)
            }
        }
    }
}