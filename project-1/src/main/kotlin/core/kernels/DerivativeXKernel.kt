package org.pdi.core.kernels

import org.pdi.core.LinearKernel

class DerivativeXKernel() : LinearKernel(3, 1) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = org.pdi.core.KernelType.CUSTOM
        kernel = arrayOf(
            floatArrayOf(-1f, 0f, 1f)
        )
    }
}