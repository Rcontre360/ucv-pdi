package org.pdi.core

enum class KernelType {
    AVERAGE,
    MEDIAN,
    GAUSSIAN,
    CUSTOM,
    LAPLACIAN,
    LAPLACIAN_GAUSSIAN,
    SOBEL_X,
    SOBEL_Y
}

abstract class Kernel(var rows: Int, var cols: Int) {
    var kernel: Array<FloatArray> = Array(rows) { FloatArray(cols) }
    var type: KernelType = KernelType.CUSTOM

    abstract fun convolute(src: Array<FloatArray>, normalize: Boolean = false): Float

    fun setKernelValue(row: Int, col: Int, value: Float) {
        if (row < rows && col < cols) {
            kernel[row][col] = value
            type = KernelType.CUSTOM
        }
    }

    abstract fun generateKernel()

    fun applyKernel(other: Kernel): Kernel {
        val newKernel = object : LinearKernel(rows, cols) {
            override fun generateKernel() {
                // Not needed for this anonymous class
            }
        }

        val rowPad = other.rows / 2
        val colPad = other.cols / 2

        for (i in 0 until cols) {
            for (j in 0 until rows) {
                var sum = 0f
                for (k in 0 until other.cols) {
                    for (l in 0 until other.rows) {
                        val thisI = i - colPad + k
                        val thisJ = j - rowPad + l

                        if (thisI >= 0 && thisJ >= 0 && thisI < cols && thisJ < rows) {
                            sum += kernel[thisI][thisJ] * other.kernel[k][l]
                        }
                    }
                }
                newKernel.kernel[i][j] = sum
            }
        }
        return newKernel
    }
}

abstract class LinearKernel(rows: Int, cols: Int) : Kernel(rows, cols) {
    override fun convolute(src: Array<FloatArray>, normalize: Boolean): Float{
        var sum = 0f
        var kernelSum = 0f
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                sum += src[i][j] * kernel[i][j]
                kernelSum += kernel[i][j]
            }
        }
        return sum / (if (normalize && kernelSum != 0f) { kernelSum } else {1f})
    }
}