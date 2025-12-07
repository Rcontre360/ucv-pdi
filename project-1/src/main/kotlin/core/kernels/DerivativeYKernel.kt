package org.pdi.core.kernels

import org.pdi.core.LinearKernel

class DerivativeYKernel() : LinearKernel(1, 3) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = org.pdi.core.KernelType.CUSTOM
        kernel = arrayOf(
            floatArrayOf(-1f),
            floatArrayOf(0f),
            floatArrayOf(1f)
        )
    }
}