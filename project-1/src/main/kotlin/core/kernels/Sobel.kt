package org.pdi.core.kernels

import org.pdi.core.LinearKernel

fun generateSobel1D(order:Int,size:Int): FloatArray{
    val gauss = (GaussianKernel.pascalRow(size - order).asList() + List(order + 1){0f}).toFloatArray()

    for (j in 0 until order){
        val gaussDx2cpy = gauss.clone()
        for (k in 0 until gauss.size) {
            val prev = if (k > 0) {
                gaussDx2cpy[k - 1]
            } else {
                0f
            }
            gauss[k] = prev - gaussDx2cpy[k]
        }
    }

    return gauss.take(size).toFloatArray()
}

class SobelXKernel(size:Int) : LinearKernel(size, size) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(true,false)

    override fun generateKernel() {
        val sobelDx = generateSobel1D(1,rows)
        val sobelDy = generateSobel1D(0,rows)

        for (i in 0 until rows){
            for (j in 0 until cols){
                kernel[i][j] = sobelDx[j] * sobelDy[i]
            }
        }
    }
}

class SobelYKernel(size:Int) : LinearKernel(size, size) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(true,false)

    override fun generateKernel() {
        val sobelDx = generateSobel1D(1,rows)
        val sobelDy = generateSobel1D(0,rows)

        for (i in 0 until rows){
            for (j in 0 until cols){
                kernel[i][j] = sobelDx[i] * sobelDy[j]
            }
        }
    }
}
