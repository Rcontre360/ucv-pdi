package org.pdi.ui

import org.pdi.core.AppState
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class FiltersPanel(private val state: AppState) : JPanel() {
    private val kernelSizeField = JTextField("3", 5)
    private val filterTypeComboBox = JComboBox<String>(arrayOf("Placeholder 1", "Placeholder 2"))

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createTitledBorder("Filters")
        preferredSize = Dimension(200, 600)

        val controlsPanel = JPanel()
        controlsPanel.layout = BoxLayout(controlsPanel, BoxLayout.Y_AXIS)

        val filterTypePanel = JPanel()
        filterTypePanel.add(JLabel("Filter Type:"))
        filterTypePanel.add(filterTypeComboBox)
        controlsPanel.add(filterTypePanel)

        val kernelSizePanel = JPanel()
        kernelSizePanel.add(JLabel("Kernel Size:"))
        kernelSizePanel.add(kernelSizeField)
        controlsPanel.add(kernelSizePanel)

        val showKernelButton = JButton("Show Kernel")
        showKernelButton.addActionListener {
            val kernelSize = kernelSizeField.text.toIntOrNull() ?: 3
            val kernel = generateKernel(kernelSize) // Using a placeholder kernel generation
            showKernelWindow(kernel)
        }
        controlsPanel.add(showKernelButton)

        val applyButton = JButton("Apply")
        applyButton.addActionListener {
            val kernelSize = kernelSizeField.text.toIntOrNull() ?: 3
            val kernel = generateKernel(kernelSize) // Using a placeholder kernel generation
            state.applyConvolution(kernel)
        }
        controlsPanel.add(applyButton)

        add(controlsPanel)
    }

    private fun generateKernel(size: Int): Array<FloatArray> {
        // Placeholder kernel generation
        return Array(size) { FloatArray(size) { 1.0f / (size * size) } }
    }

    private fun showKernelWindow(kernel: Array<FloatArray>) {
        val kernelFrame = JFrame("Kernel Matrix")
        kernelFrame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        kernelFrame.contentPane.add(KernelMatrixPanel(kernel))
        kernelFrame.pack()
        kernelFrame.isVisible = true
    }
}
