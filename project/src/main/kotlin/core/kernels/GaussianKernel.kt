package org.pdi.core.kernels

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.pdi.core.image.release
import org.pdi.core.image.rgbToYuv
import org.pdi.core.image.yuvToRGB
import org.pdi.core.transforms.DFT

// gaussian kernel, generated using the pascal row
class GaussianKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    // researched that companion objects are like static functions
    companion object{
        // creates just a ROW of the pascal triangle
         fun pascalRow(size: Int): FloatArray {
            if (size <= 0)
                return floatArrayOf()

            val n = size - 1
            val row = FloatArray(size)
            // first value is one
            row[0] = 1.0f

            for (i in 1..n)
                row[i] = row[i - 1] * (n.toFloat() - i.toFloat() + 1.0f) / i.toFloat()

            return row
        }
    }

    // for gaussian we
    override fun generateKernel() {
        // to generate it we create the outer product of the two pascal vectors
        val pascal1DRows = pascalRow(rows)
        val pascal1DCols = pascalRow(cols)

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val value = pascal1DRows[i] * pascal1DCols[j]
                kernel[i][j] = value
            }
        }
    }

    fun revert(input: Mat, k: Float): Mat {
        val dftTool = DFT()
        val yuvChannels = rgbToYuv(input)
        val workingMat = dftTool.toFrequency(yuvChannels[0]) // imageDFT

        val kernelMat = Mat.zeros(yuvChannels[0].size(), CvType.CV_32F)
        val normalized = this.normalize()

        // we need to put the kernel on each of the corners (like splitted) for this to work
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val r = (i - rows / 2 + kernelMat.rows()) % kernelMat.rows()
                val c = (j - cols / 2 + kernelMat.cols()) % kernelMat.cols()
                kernelMat.put(r, c, normalized[i][j].toDouble())
            }
        }

        val kernelDFT = dftTool.toFrequency(kernelMat)
        val wienerFilter = wiener(kernelDFT, k.toDouble())

        Core.mulSpectrums(workingMat, wienerFilter, workingMat, 0)
        Core.idft(workingMat, workingMat, Core.DFT_REAL_OUTPUT + Core.DFT_SCALE)
        workingMat.convertTo(workingMat, CvType.CV_8U)

        val result = yuvToRGB(listOf(workingMat, yuvChannels[1], yuvChannels[2]))
        listOf(wienerFilter,kernelDFT,kernelMat, workingMat).release()
        yuvChannels.release()

        return result
    }

    private fun wiener(kernelDFT: Mat, k: Double): Mat {
        val planes = mutableListOf<Mat>()
        Core.split(kernelDFT, planes) // planes[0] = Real, planes[1] = Imag

        val den = Mat()

        Core.magnitude(planes[0], planes[1], den) //|H| = sqrt(Real² + Imag²)
        Core.multiply(den, den, den) // |H|²
        Core.add(den, Scalar(k), den) // |H|² + K
        Core.divide(planes[0], den, planes[0]) // Real(W) = Real(H) / (|H|² + K)
        Core.divide(planes[1], den, planes[1]) // Imag(W) = Imag(H) / (|H|² + K)
        Core.multiply(planes[1], Scalar(-1.0), planes[1]) // (a + bi) to (a - bi)

        val wienerFilter = Mat()
        Core.merge(planes, wienerFilter)

        den.release()
        planes.release()

        return wienerFilter
    }
}