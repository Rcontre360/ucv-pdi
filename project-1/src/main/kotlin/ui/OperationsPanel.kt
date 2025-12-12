package org.pdi.ui

import org.pdi.core.AppState
import org.pdi.core.Kernel
import org.pdi.core.kernels.PrewittXKernel
import org.pdi.core.kernels.PrewittYKernel
import org.pdi.core.kernels.RobertsXKernel
import org.pdi.core.kernels.RobertsYKernel
import org.pdi.core.kernels.SobelXKernel
import org.pdi.core.kernels.SobelYKernel
import java.awt.BorderLayout
import javax.swing.JComboBox
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

enum class BorderDetectionType {
    SOBEL,
    ROBERTS,
    PREWITT
}

class OperationsPanel(private val state: AppState) : JPanel() {
    private val operationsComboBox = JComboBox<BorderDetectionType>(BorderDetectionType.values())
    private val applyButton = JButton("Apply")
    private var selectedOperation: BorderDetectionType = BorderDetectionType.SOBEL

    init {
        layout = BorderLayout()
        preferredSize = Dimension(200, 100)
        border = BorderFactory.createTitledBorder("Operations")
        add(operationsComboBox, BorderLayout.NORTH)
        add(applyButton, BorderLayout.SOUTH)

        operationsComboBox.addActionListener {
            selectedOperation = operationsComboBox.selectedItem as BorderDetectionType
        }

        applyButton.addActionListener {
            selectedOperation?.let {
                val (kernelX,kernelY) = when (selectedOperation) {
                    BorderDetectionType.SOBEL -> Pair(SobelXKernel(3), SobelYKernel(3))
                    BorderDetectionType.ROBERTS -> Pair(RobertsXKernel(), RobertsYKernel())
                    BorderDetectionType.PREWITT -> Pair(PrewittXKernel(), PrewittYKernel())
                }
                state.applyBorderOperator(kernelX,kernelY)
            }
        }
    }
}
