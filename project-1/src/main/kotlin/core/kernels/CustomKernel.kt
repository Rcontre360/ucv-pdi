package org.pdi.core.kernels

import org.pdi.core.LinearKernel

class CustomKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    override fun generateKernel() {
        this.type = org.pdi.core.KernelType.CUSTOM
        kernel = Array(rows) { FloatArray(cols) { 0f } }
    }
}