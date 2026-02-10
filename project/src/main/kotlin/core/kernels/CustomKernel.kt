package org.pdi.core.kernels

// its like the default kernel we fallback to, fully customizable
class CustomKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    override fun generateKernel() {
        kernel = Array(rows) { FloatArray(cols) { 0f } }
    }
}