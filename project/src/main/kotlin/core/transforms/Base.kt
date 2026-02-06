package org.pdi.core.transforms

import org.opencv.core.Mat

interface Transform {
    fun generateFrequencyMat(mat: Mat): Mat
    fun apply(mat: Mat, threshold: Double, highPass: Boolean): Mat
    fun createFilter(rows: Int, cols: Int, threshold: Double, highPass: Boolean): Mat
}