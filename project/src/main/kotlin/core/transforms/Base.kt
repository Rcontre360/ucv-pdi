package org.pdi.core.transforms

import org.opencv.core.Mat

abstract class Transform {
    fun generateFrequencyMat(mat: Mat): Mat {
        val dct = toFrequency(mat)
        val magnitude = logMagnitude(dct)
        dct.release()
        return magnitude
    }

    abstract fun toFrequency(mat: Mat): Mat
    abstract fun logMagnitude(mat: Mat): Mat
    abstract fun applyFilter(mat: Mat, threshold: Double, highPass: Boolean): Mat
    abstract fun createFilter(rows: Int, cols: Int, threshold: Double, highPass: Boolean): Mat
}