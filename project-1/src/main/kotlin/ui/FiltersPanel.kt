package org.pdi.ui

import org.pdi.core.*
import org.pdi.core.kernels.*
import java.awt.Dimension
import javax.swing.*

class FiltersPanel(private val state: AppState) : JPanel() {
    private val rowsField = JTextField("3", 5)
    private val colsField = JTextField("3", 5)
    private val typeComboBox = JComboBox<KernelType>(KernelType.values())
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
        filterTypePanel.add(typeComboBox)
        controlsPanel.add(filterTypePanel)

        typeComboBox.addActionListener {
            val kernel = createKernel()
            val (customRows,customCols) = kernel.isCustomizable()
            rowsField.isEditable = customRows
            colsField.isEditable = customCols
        }

        val kernelSizePanel = JPanel()
        kernelSizePanel.add(JLabel("Kernel Rows:"))
        kernelSizePanel.add(rowsField)
        kernelSizePanel.add(JLabel("Kernel Cols:"))
        kernelSizePanel.add(colsField)
        controlsPanel.add(kernelSizePanel)
        controlsPanel.add(profilingFactorAdjuster)

        val showKernelButton = JButton("Show Kernel")
        showKernelButton.addActionListener {
            showKernelWindow(createKernel())
        }
        controlsPanel.add(showKernelButton)

        val applyButton = JButton("Apply")
        applyButton.addActionListener {
            state.applyConvolution(createKernel())
        }
        controlsPanel.add(applyButton)

        add(controlsPanel)

        rowsField.text = "3"
        colsField.text = "3"
    }

    private fun createKernel(): Kernel {
        val rows = rowsField.text.toIntOrNull() ?: 3
        val cols = colsField.text.toIntOrNull() ?: 3
        val selected = typeComboBox.selectedItem as KernelType
        val kernel = when (selected) {
            KernelType.CUSTOM -> CustomKernel(rows,cols)
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
