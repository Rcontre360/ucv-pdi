package org.pdi.core.kernels

import java.awt.image.BufferedImage
import org.pdi.core.Kernel

class MedianKernel(rows: Int, cols: Int) : Kernel(rows, cols) {

    override fun generateKernel() {
        this.type = org.pdi.core.KernelType.MEDIAN
        kernel = Array(rows) { FloatArray(cols) { 1f } }
    }

    override fun convolute(src: Array<FloatArray>): Float{
        var list = mutableListOf<Float>()
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                list.add(src[i][j])
            }
        }
        list.sort()
        return list[list.size / 2]
    }
}
