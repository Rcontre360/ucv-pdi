package org.pdi.core.kernels

import org.pdi.core.LinearKernel

class GaussianKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    private fun pascalRow(size: Int): FloatArray {
        if (size <= 0) return floatArrayOf()
        val n = size - 1
        val row = FloatArray(size)

        row[0] = 1.0f

        for (k in 1..n) {
            row[k] = row[k - 1] * (n.toFloat() - k.toFloat() + 1.0f) / k.toFloat()
        }
        return row
    }

    override fun generateKernel() {
        this.type = org.pdi.core.KernelType.GAUSSIAN
        val pascal1DRows = pascalRow(rows)
        val pascal1DCols = pascalRow(cols)
        var sum = 0.0f

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val value = pascal1DRows[i] * pascal1DCols[j]
                kernel[i][j] = value
                sum += value
            }
        }
    }
}