package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.Kernel
import org.pdi.core.KernelType

class SobelXKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.SOBEL_X
        val gaussian = GaussianKernel(rows, cols)
        gaussian.generateKernel()
        val derivativeX = DerivativeXKernel()
        val sobelX = gaussian.applyKernel(derivativeX)
        this.kernel = sobelX.kernel
        this.rows = sobelX.rows
        this.cols = sobelX.cols
    }
}