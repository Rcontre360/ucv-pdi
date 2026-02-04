package org.pdi.core.kernels

// median kernel, the only kernel where convolution is different
class MedianKernel(rows: Int, cols: Int) : Kernel(rows, cols) {

    // is customizable
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(true,true)

    // easy to generate
    override fun generateKernel() {
        kernel = Array(rows) { FloatArray(cols) { 1f } }
    }

    // sort all values and pick the middle one
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
