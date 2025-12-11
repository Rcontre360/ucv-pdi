package org.pdi.core.kernels

import org.pdi.core.LinearKernel

class DerivativeYKernel(rows:Int) : LinearKernel(rows, 1) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(true,false)

    override fun generateKernel() {
        kernel = if (rows < 2) {
            throw IllegalArgumentException("rows under 2 for Dx kernel")
        } else if (rows == 2) {
            arrayOf(
                floatArrayOf(-1f),
                floatArrayOf(1f)
            )
        } else {
            val mid = rows / 2
            Array(rows) { i ->
                floatArrayOf((i - mid).toFloat())
            }
        }
    }
}