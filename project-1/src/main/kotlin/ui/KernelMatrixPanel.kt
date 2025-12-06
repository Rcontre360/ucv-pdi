package org.pdi.ui

import org.pdi.core.Kernel
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingConstants

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class KernelMatrixPanel(private val kernel: Kernel) : JPanel() {
    init {
        val rows = kernel.rows
        val cols = kernel.cols
        layout = GridLayout(rows, cols, 5, 5)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val field = JTextField(kernel.kernel[i][j].toString())
                field.horizontalAlignment = SwingConstants.CENTER
                field.isEditable = true
                field.document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent?) {
                        updateKernelValue()
                    }

                    override fun removeUpdate(e: DocumentEvent?) {
                        updateKernelValue()
                    }

                    override fun changedUpdate(e: DocumentEvent?) {
                        updateKernelValue()
                    }

                    private fun updateKernelValue() {
                        val newValue = field.text.toFloatOrNull()
                        if (newValue != null) {
                            kernel.setKernelValue(i, j, newValue)
                        }
                    }
                })
                add(field)
            }
        }
    }
}
