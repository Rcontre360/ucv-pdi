package org.pdi.core.kernels

import kotlin.math.pow

// average kernel, is just 1's everywhere forming a CIRCLE instead of a square
class AverageKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    override fun generateKernel() {
        val centerX = cols / 2.0
        val centerY = rows / 2.0
        val radius = minOf(rows, cols) / 2.0

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val distance = kotlin.math.sqrt((i - centerY + 0.5).pow(2) + (j - centerX + 0.5).pow(2))
                if (distance <= radius) {
                    kernel[i][j] = 1f
                } else {
                    kernel[i][j] = 0f
                }
            }
        }
    }
}