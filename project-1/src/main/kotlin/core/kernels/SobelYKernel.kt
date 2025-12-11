package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.Kernel
import org.pdi.core.KernelType

class SobelYKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    override fun generateKernel() {
        val gaussian = GaussianKernel(1, cols)
        val derivativeY = DerivativeYKernel(rows)
        val sobelXmat = derivativeY * gaussian

        this.kernel = sobelXmat
        this.rows = sobelXmat[0].size
        this.cols = sobelXmat.size
    }
}