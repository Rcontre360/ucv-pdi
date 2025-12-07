package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.KernelType

class RobertsYKernel : LinearKernel(2, 2) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.CUSTOM
        kernel = arrayOf(
            floatArrayOf(0f, 1f),
            floatArrayOf(-1f, 0f)
        )
    }
}