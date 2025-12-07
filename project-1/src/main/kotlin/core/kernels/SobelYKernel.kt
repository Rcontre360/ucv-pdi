package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.Kernel
import org.pdi.core.KernelType

class SobelYKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.SOBEL_Y
        val gaussian = GaussianKernel(rows, cols)
        gaussian.generateKernel()
        val derivativeY = DerivativeYKernel()
        val sobelY = gaussian.applyKernel(derivativeY)
        this.kernel = sobelY.kernel
        this.rows = sobelY.rows
        this.cols = sobelY.cols
    }
}