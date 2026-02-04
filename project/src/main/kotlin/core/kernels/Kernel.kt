package org.pdi.core.kernels

/// all available kernel types
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
    PREWITT_Y,
    ERODE,
    DILATE
}

// abstract kernel class, initializes with its size
abstract class Kernel(var rows: Int, var cols: Int) {
    var kernel: Array<FloatArray> = Array(rows) { FloatArray(cols) }

    // we abstract convolute because of the median kernel which is different from the others
    abstract fun convolute(src: Array<FloatArray>): Float
    // generate kernel builds the kernel values
    abstract fun generateKernel()
    // returns (row,col) if its customizable on the rows or columns. This is bc we have one kernel which is static
    // If only rows is customizable it means this is a squared kernel
    abstract fun isCustomizable():Pair<Boolean,Boolean>

    init {
        generateKernel()
    }

    // used for the panel to edit kernels
    fun setKernelValue(row: Int, col: Int, value: Float) {
        // can be improved since this fails silently
        if (row < rows && col < cols) {
            kernel[row][col] = value
        }
    }
}

// linear kernel, most kernels inherit from this one
abstract class LinearKernel(rows: Int, cols: Int) : Kernel(rows, cols) {
    // by default kernels are customizable
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(true,true)

    // standard convolute operation. Receives the other square to use for multiplication+sum
    override fun convolute(src: Array<FloatArray>): Float{
        var sum = 0f
        var kernelSum = 0f
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                sum += src[i][j] * kernel[i][j]
                kernelSum += kernel[i][j]
            }
        }
        // this kernel always divides over the sum. If the kernel sum is 0 it divides by 1
        // the comparison with 0 is made like this because kernelSum is float
        return sum / (if (kernelSum !in 0f..0f) { kernelSum } else {1f})
    }
}
