package org.pdi.ui

import org.pdi.core.AppState
import org.pdi.core.Sobel
import java.awt.BorderLayout
import javax.swing.JComboBox
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

class OperationsPanel(private val state: AppState) : JPanel() {
    private val operations = arrayOf("Sobel", "Roberts", "Prewitt", "Gradient")
    private val operationsComboBox = JComboBox(operations)
    private val applyButton = JButton("Apply")
    private var selectedOperation: org.pdi.core.Operation? = null

    init {
        layout = BorderLayout()
        preferredSize = Dimension(200, 100)
        border = BorderFactory.createTitledBorder("Operations")
        add(operationsComboBox, BorderLayout.NORTH)
        add(applyButton, BorderLayout.SOUTH)

        operationsComboBox.addActionListener {
            selectedOperation = when (operationsComboBox.selectedItem as String) {
                "Sobel" -> Sobel()
                else -> null
            }
        }

        applyButton.addActionListener {
            selectedOperation?.let {
                state.applyOperation(it)
            }
        }
    }
}
