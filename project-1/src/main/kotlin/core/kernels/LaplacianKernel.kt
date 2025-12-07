package org.pdi.core.kernels

import org.pdi.core.LinearKernel

class LaplacianKernel : LinearKernel(3, 3) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = org.pdi.core.KernelType.LAPLACIAN
        kernel = arrayOf(
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(1f, -4f, 1f),
            floatArrayOf(0f, 1f, 0f)
        )
    }
}