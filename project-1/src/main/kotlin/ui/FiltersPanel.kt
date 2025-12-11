package org.pdi.ui

import org.pdi.core.*
import org.pdi.core.kernels.*
import java.awt.Dimension
import javax.swing.*

class FiltersPanel(private val state: AppState) : JPanel() {
    private val kernelRowsField = JTextField("3", 5)
    private val kernelColsField = JTextField("3", 5)
    private val filterTypeComboBox = JComboBox<KernelType>(KernelType.values())

    private var selectedKernelType: KernelType = KernelType.GAUSSIAN
    private var customKernel: Kernel? = null
    private val laplacianProfilingKernel = LaplacianKernelProfiling()

    private val profilingFactorAdjuster = ValueAdjuster(1f, 1f, 10f, 1f) { newValue ->
        laplacianProfilingKernel.updateFactor(newValue.toInt())
        state.applyConvolution(laplacianProfilingKernel)
    }.apply {
        border = BorderFactory.createTitledBorder("Profiling Factor")
        isVisible = false // Initially hidden
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createTitledBorder("Filters")
        preferredSize = Dimension(200, 300)
        maximumSize = Dimension(Int.MAX_VALUE, 300)

        val controlsPanel = JPanel()
        controlsPanel.layout = BoxLayout(controlsPanel, BoxLayout.Y_AXIS)

        val filterTypePanel = JPanel()
        filterTypePanel.add(JLabel("Filter Type:"))
        filterTypePanel.add(filterTypeComboBox)
        controlsPanel.add(filterTypePanel)

        filterTypeComboBox.addActionListener {
            selectedKernelType = filterTypeComboBox.selectedItem as KernelType
            val isFixedSizeKernel = when (selectedKernelType) {
                KernelType.LAPLACIAN -> {
                    kernelRowsField.text = "3"
                    kernelColsField.text = "3"
                    profilingFactorAdjuster.isVisible = false
                    true
                }
                KernelType.LAPLACIAN_PROFILING -> {
                    kernelRowsField.text = "3"
                    kernelColsField.text = "3"
                    profilingFactorAdjuster.isVisible = true
                    true
                }
                KernelType.ROBERTS_X, KernelType.ROBERTS_Y -> {
                    kernelRowsField.text = "2"
                    kernelColsField.text = "2"
                    profilingFactorAdjuster.isVisible = false
                    true
                }
                KernelType.PREWITT_X, KernelType.PREWITT_Y -> {
                    kernelRowsField.text = "3"
                    kernelColsField.text = "3"
                    profilingFactorAdjuster.isVisible = false
                    true
                }
                KernelType.SOBEL_X, KernelType.SOBEL_Y -> {
                    kernelRowsField.text = "3"
                    kernelColsField.text = "3"
                    profilingFactorAdjuster.isVisible = false
                    true
                }
                else -> {
                    profilingFactorAdjuster.isVisible = false
                    false
                }
            }
            kernelRowsField.isEditable = !isFixedSizeKernel
            kernelColsField.isEditable = !isFixedSizeKernel
        }

        val kernelSizePanel = JPanel()
        kernelSizePanel.add(JLabel("Kernel Rows:"))
        kernelSizePanel.add(kernelRowsField)
        kernelSizePanel.add(JLabel("Kernel Cols:"))
        kernelSizePanel.add(kernelColsField)
        controlsPanel.add(kernelSizePanel)

        kernelRowsField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { updateSize() }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { updateSize() }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { updateSize() }
            fun updateSize() {
                // currentRows = kernelRowsField.text.toIntOrNull() ?: 3 // Removed
            }
        })
        kernelColsField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { updateSize() }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { updateSize() }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { updateSize() }
            fun updateSize() {
                // currentCols = kernelColsField.text.toIntOrNull() ?: 3 // Removed
            }
        })

        controlsPanel.add(profilingFactorAdjuster)

        val showKernelButton = JButton("Show Kernel")
        showKernelButton.addActionListener {
            val rows = kernelRowsField.text.toIntOrNull()
            val cols = kernelColsField.text.toIntOrNull()
            if (rows == null || cols == null || rows !in 1..13 || cols !in 1..13) {
                JOptionPane.showMessageDialog(this, "Kernel rows and columns must be integers between 1 and 7.", "Invalid Input", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            val kernelToShow = createKernel()
            showKernelWindow(kernelToShow)
        }
        controlsPanel.add(showKernelButton)

        val applyButton = JButton("Apply")
        applyButton.addActionListener {
            val rows = kernelRowsField.text.toIntOrNull()
            val cols = kernelColsField.text.toIntOrNull()
            if (rows == null || cols == null || rows !in 1..13 || cols !in 1..13) {
                JOptionPane.showMessageDialog(this, "Kernel rows and columns must be integers between 1 and 7.", "Invalid Input", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            val kernelToApply = createKernel()
            state.applyConvolution(kernelToApply)
        }
        controlsPanel.add(applyButton)

        add(controlsPanel)

        filterTypeComboBox.selectedItem = selectedKernelType
        kernelRowsField.text = "3" // Initial value
        kernelColsField.text = "3" // Initial value
    }

    private fun createKernel(): Kernel {
        val rows = kernelRowsField.text.toIntOrNull() ?: 3
        val cols = kernelColsField.text.toIntOrNull() ?: 3
        if (selectedKernelType == KernelType.CUSTOM) {
            if (customKernel == null || customKernel!!.rows != rows || customKernel!!.cols != cols) {
                customKernel = CustomKernel(rows, cols)
                customKernel!!.generateKernel()
            }
            return customKernel as Kernel
        }
        val kernel = when (selectedKernelType) {
            KernelType.AVERAGE -> AverageKernel(rows, cols)
            KernelType.MEDIAN -> MedianKernel(rows, cols)
            KernelType.GAUSSIAN -> GaussianKernel(rows, cols)
            KernelType.LAPLACIAN -> LaplacianKernel()
            KernelType.LAPLACIAN_PROFILING -> {
                laplacianProfilingKernel.rows = rows
                laplacianProfilingKernel.cols = cols
                laplacianProfilingKernel.generateKernel()
                laplacianProfilingKernel
            }
            KernelType.SOBEL_X -> SobelXKernel(rows, cols)
            KernelType.SOBEL_Y -> SobelYKernel(rows, cols)
            KernelType.ROBERTS_X -> RobertsXKernel()
            KernelType.ROBERTS_Y -> RobertsYKernel()
            KernelType.PREWITT_X -> PrewittXKernel()
            KernelType.PREWITT_Y -> PrewittYKernel()
            else -> {
                object : LinearKernel(rows, cols) {
                    override fun generateKernel() {}
                }
            }
        }
        kernel.generateKernel()
        return kernel
    }

    private fun showKernelWindow(kernel: Kernel) {
        val kernelFrame = JFrame("Kernel Matrix")
        kernelFrame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        val kernelMatrixPanel = KernelMatrixPanel(kernel)
        kernelFrame.contentPane.add(kernelMatrixPanel)
        kernelFrame.pack()
        kernelFrame.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this))
        kernelFrame.isVisible = true
    }
}
