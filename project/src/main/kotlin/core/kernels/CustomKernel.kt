package org.pdi.core.kernels

import org.pdi.core.LinearKernel

// its like the default kernel we fallback to, fully customizable
class CustomKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    override fun generateKernel() {
        kernel = Array(rows) { FloatArray(cols) { 0f } }
    }
}