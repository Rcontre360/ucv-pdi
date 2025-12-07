package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.KernelType

class LaplacianKernel : LinearKernel(3, 3) {
    init {
        generateKernel()
    }

    override fun generateKernel() {
        this.type = KernelType.LAPLACIAN
        kernel = arrayOf(
            floatArrayOf(-1f, -1f, -1f),
            floatArrayOf(-1f, 8f, -1f),
            floatArrayOf(-1f, -1f, -1f)
        )
    }
}
