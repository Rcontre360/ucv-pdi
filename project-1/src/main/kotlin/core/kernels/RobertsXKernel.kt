package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.KernelType

class RobertsXKernel : LinearKernel(2, 2) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(false,false)

    override fun generateKernel() {
        kernel = arrayOf(
            floatArrayOf(1f, 0f),
            floatArrayOf(0f, -1f)
        )
    }
}