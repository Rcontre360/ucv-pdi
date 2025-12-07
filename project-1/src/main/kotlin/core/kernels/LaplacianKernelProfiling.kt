package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.KernelType

class LaplacianKernelProfiling : LinearKernel(3, 3) {
    var profilingFactor = 1f

    init {
        generateKernel()
    }

    fun updateFactor(factor: Int){
        profilingFactor = factor.toFloat()
        generateKernel()
    }

    override fun generateKernel() {
        this.type = KernelType.LAPLACIAN // Still LAPLACIAN type, but with profiling
        kernel = arrayOf(
            floatArrayOf(0f, profilingFactor, 0f),
            floatArrayOf(profilingFactor, -4f * profilingFactor + 1, profilingFactor),
            floatArrayOf(0f, profilingFactor, 0f)
        )
    }
}