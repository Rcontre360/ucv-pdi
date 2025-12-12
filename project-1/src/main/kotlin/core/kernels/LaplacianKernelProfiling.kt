package org.pdi.core.kernels

import org.pdi.core.LinearKernel
import org.pdi.core.KernelType

class LaplacianKernelProfiling : LinearKernel(3, 3) {
    var profilingFactor = 1f

    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(false,false)

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