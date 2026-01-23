package org.pdi.core.kernels

import org.pdi.core.LinearKernel

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
}