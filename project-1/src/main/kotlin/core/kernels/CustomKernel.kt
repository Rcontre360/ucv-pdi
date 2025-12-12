package org.pdi.core.kernels

import org.pdi.core.Kernel
import org.pdi.core.LinearKernel

class CustomKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    override fun generateKernel() {
        kernel = Array(rows) { FloatArray(cols) { 0f } }
    }
}