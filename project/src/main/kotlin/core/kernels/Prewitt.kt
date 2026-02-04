package org.pdi.core.kernels

// we use static prewitt kernels since the prewitt operator for border detection is defined with these kernels
// see https://en.wikipedia.org/wiki/Prewitt_operator
// in general all kernels used for border detection using the image gradient are static. Except for Sobel since
// we found its dynamic implementation

class PrewittXKernel : LinearKernel(3, 3) {
    init {
        generateKernel()
    }

    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(false,false)

    override fun generateKernel() {
        kernel = arrayOf(
            floatArrayOf(-1f, 0f, 1f),
            floatArrayOf(-1f, 0f, 1f),
            floatArrayOf(-1f, 0f, 1f)
        )
    }
}

class PrewittYKernel : LinearKernel(3, 3) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(false,false)

    override fun generateKernel() {
        kernel = arrayOf(
            floatArrayOf(-1f, -1f, -1f),
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(1f, 1f, 1f)
        )
    }
}