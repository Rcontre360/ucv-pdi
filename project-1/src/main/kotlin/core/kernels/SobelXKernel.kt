package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.Kernel
import org.pdi.core.KernelType

class SobelXKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    override fun generateKernel() {
        val gaussian = GaussianKernel(rows, 1)
        val derivativeX = DerivativeXKernel(cols)
        val sobelXmat = gaussian  * derivativeX

        this.kernel = sobelXmat
        this.rows = sobelXmat[0].size
        this.cols = sobelXmat.size
    }
}