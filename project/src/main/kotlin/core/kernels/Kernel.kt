package org.pdi.core.kernels

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.pdi.core.transforms.DFT

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

class KernelConfigManager(private val laplacianProfiling: LaplacianKernelProfiling) {
    fun createInstance(rows: Int, cols: Int, type: KernelType): Kernel {
        val kernel = when (type) {
            KernelType.CUSTOM -> CustomKernel(rows, cols)
            KernelType.AVERAGE -> AverageKernel(rows, cols)
            KernelType.MEDIAN -> MedianKernel(rows, cols)
            KernelType.GAUSSIAN -> GaussianKernel(rows, cols)
            KernelType.LAPLACIAN -> LaplacianKernel(rows)
            KernelType.LAPLACIAN_PROFILING -> laplacianProfiling.apply {
                this.rows = rows; this.cols = cols; generateKernel()
            }
            KernelType.SOBEL_X -> SobelXKernel(rows)
            KernelType.SOBEL_Y -> SobelYKernel(rows)
            KernelType.ROBERTS_X -> RobertsXKernel()
            KernelType.ROBERTS_Y -> RobertsYKernel()
            KernelType.PREWITT_X -> PrewittXKernel()
            KernelType.PREWITT_Y -> PrewittYKernel()
            KernelType.ERODE -> ErodeKernel(rows, cols)
            KernelType.DILATE -> DilateKernel(rows, cols)
        }
        if (type != KernelType.LAPLACIAN_PROFILING) kernel.generateKernel()
        return kernel
    }
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

    fun sum(): Float{
        var kernelSum = 0.0f
        for (i in 0 until rows) {
            for (j in 0 until cols) kernelSum += kernel[i][j]
        }
        return kernelSum
    }

    fun normalize(): Array<FloatArray> {
        val normalizedMatrix = Array(rows) { i -> kernel[i].copyOf() }
        val kernelSum = this.sum()

        if (kernelSum == 0.0f) return normalizedMatrix

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                normalizedMatrix[i][j] = kernel[i][j] / kernelSum
            }
        }

        return normalizedMatrix
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
