package org.pdi.core.kernels

import org.pdi.core.LinearKernel

class LaplacianKernel : LinearKernel(3, 3) {
    var profilingFactor = 1f

    init {
        generateKernel()
    }

    override fun generateKernel() {
        this.type = org.pdi.core.KernelType.LAPLACIAN
        kernel = arrayOf(
            floatArrayOf(0f, profilingFactor, 0f),
            floatArrayOf(profilingFactor, -4f * profilingFactor, profilingFactor),
            floatArrayOf(0f, profilingFactor, 0f)
        )
    }
}