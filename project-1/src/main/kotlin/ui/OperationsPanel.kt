package org.pdi.ui

import org.pdi.core.AppState
import org.pdi.core.BorderDetection
import org.pdi.core.BorderDetectionType
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

class OperationsPanel(private val state: AppState) : JPanel() {
    private val operationsComboBox = JComboBox<BorderDetectionType>(BorderDetectionType.values())
    private val applyButton = JButton("Apply")
    private var selectedOperation: org.pdi.core.BorderDetectionType = BorderDetectionType.SOBEL

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
                val operation = when (selectedOperation) {
                    BorderDetectionType.SOBEL -> BorderDetection(SobelXKernel(3,3), SobelYKernel(3,3))
                    BorderDetectionType.ROBERTS -> BorderDetection(RobertsXKernel(), RobertsYKernel())
                    BorderDetectionType.PREWITT -> BorderDetection(PrewittXKernel(), PrewittYKernel())
                }
                state.applyOperation(operation)
            }
        }
    }
}
