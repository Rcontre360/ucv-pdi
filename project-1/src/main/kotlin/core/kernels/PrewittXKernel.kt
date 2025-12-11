package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.KernelType

class PrewittXKernel : LinearKernel(3, 3) {
    init {
        generateKernel()
    }

    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(false,false)

    override fun generateKernel() {
        kernel = arrayOf(
            floatArrayOf(-1f, 0f, 1f),
            floatArrayOf(-1f, 0f, 1f),
            floatArrayOf(-1f, 0f, 1f)
        )
    }
}