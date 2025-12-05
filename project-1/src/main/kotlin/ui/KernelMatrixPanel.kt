package org.pdi.ui

import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingConstants

class KernelMatrixPanel(kernel: Array<FloatArray>) : JPanel() {
    init {
        val size = kernel.size
        layout = GridLayout(size, size, 5, 5)
        for (i in 0 until size) {
            for (j in 0 until size) {
                val field = JTextField(kernel[i][j].toString())
                field.horizontalAlignment = SwingConstants.CENTER
                field.isEditable = false
                add(field)
            }
        }
    }
}
