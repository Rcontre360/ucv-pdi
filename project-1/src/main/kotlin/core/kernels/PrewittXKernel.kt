package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.KernelType

class PrewittXKernel : LinearKernel(3, 3) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.CUSTOM
        kernel = arrayOf(
            floatArrayOf(-1f, 0f, 1f),
            floatArrayOf(-1f, 0f, 1f),
            floatArrayOf(-1f, 0f, 1f)
        )
    }
}