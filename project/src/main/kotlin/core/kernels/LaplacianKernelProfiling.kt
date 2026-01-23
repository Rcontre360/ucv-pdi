package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.KernelType

// this kernel is only to make profiling with the laplacian one. So we dont make it dynamic
class LaplacianKernelProfiling : LinearKernel(3, 3) {
    var profilingFactor = 1f

    // not customizable since its static
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(false,false)

    // update the factor and regenerate the kernel
    fun updateFactor(factor: Int){
        profilingFactor = factor.toFloat()
        generateKernel()
    }

    override fun generateKernel() {
        kernel = arrayOf(
            floatArrayOf(0f, profilingFactor, 0f),
            floatArrayOf(profilingFactor, -4f * profilingFactor + 1, profilingFactor),
            floatArrayOf(0f, profilingFactor, 0f)
        )
    }
    
}