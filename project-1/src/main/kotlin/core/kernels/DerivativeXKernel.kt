package org.pdi.core.kernels

import org.pdi.core.LinearKernel

class DerivativeXKernel(cols:Int) : LinearKernel(1, cols) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(true,false)

    override fun generateKernel() {
        val row: FloatArray = if (cols < 2) {
            throw IllegalArgumentException("rows under 2 for Dx kernel")
        } else if (cols == 2) {
            floatArrayOf(-1f, 1f)
        } else {
             val mid = cols / 2
            FloatArray(cols) { i ->
                (i - mid).toFloat()
            }
        }

        kernel = arrayOf(
            row
        )
    }
}