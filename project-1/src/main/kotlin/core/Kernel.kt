package org.pdi.core

enum class KernelType {
    AVERAGE,
    MEDIAN,
    GAUSSIAN,
    CUSTOM,
    LAPLACIAN,
    LAPLACIAN_PROFILING,
    SOBEL_X,
    SOBEL_Y,
    ROBERTS_X,
    ROBERTS_Y,
    PREWITT_X,
    PREWITT_Y
}

abstract class Kernel(var rows: Int, var cols: Int) {
    var kernel: Array<FloatArray> = Array(rows) { FloatArray(cols) }

    abstract fun convolute(src: Array<FloatArray>): Float
    abstract fun generateKernel()
    abstract fun isCustomizable():Pair<Boolean,Boolean>

    init {
        generateKernel()
    }

    fun get(row: Int, col:Int)  = kernel[row][col]

    operator fun times(other: Kernel): Array<FloatArray> {
        if (this.cols != other.rows) {
            throw IllegalArgumentException(
                "matmul dimensions mismatch: " + "this.cols(${this.cols}) and other.rows(${other.rows})."
            )
        }

        val resRows = this.rows
        val resCols = other.cols
        val result = Array(resRows) { FloatArray(resCols) }

        for (i in 0 until resRows) {
            for (j in 0 until resCols) {
                var sum = 0f
                for (k in 0 until this.cols) {
                    sum += this.kernel[i][k] * other.kernel[k][j]
                }
                result[i][j] = sum
            }
        }

        return result
    }

    fun setKernelValue(row: Int, col: Int, value: Float) {
        if (row < rows && col < cols) {
            kernel[row][col] = value
        }
    }
}

abstract class LinearKernel(rows: Int, cols: Int) : Kernel(rows, cols) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(true,true)

    override fun convolute(src: Array<FloatArray>): Float{
        var sum = 0f
        var kernelSum = 0f
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                sum += src[i][j] * kernel[i][j]
                kernelSum += kernel[i][j]
            }
        }
        return sum / (if (kernelSum !in 0f..0f) { kernelSum } else {1f})
    }
}
