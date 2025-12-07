package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.Kernel
import org.pdi.core.KernelType

class LaplacianGaussianKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.LAPLACIAN_GAUSSIAN
        val gaussian = GaussianKernel(rows, cols)
        gaussian.generateKernel()

        val laplacian = LaplacianKernel()
        val logKernel = gaussian.applyKernel(laplacian)
        this.kernel = logKernel.kernel
        this.rows = logKernel.rows
        this.cols = logKernel.cols
    }
}