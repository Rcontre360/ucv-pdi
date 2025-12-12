package org.pdi.core.kernels

import org.pdi.core.LinearKernel

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

class PrewittYKernel : LinearKernel(3, 3) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(false,false)

    override fun generateKernel() {
        kernel = arrayOf(
            floatArrayOf(-1f, -1f, -1f),
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(1f, 1f, 1f)
        )
    }
}