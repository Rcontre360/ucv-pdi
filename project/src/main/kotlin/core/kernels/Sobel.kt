package org.pdi.core.kernels

import org.pdi.core.LinearKernel

// same sources as the laplacian kernel
// https://github.com/opencv/opencv/blob/4.x/modules/ts/src/ts_func.cpp
// this function generates a gaussian row of size x and applies the derivative "order" times
// so this will return the derivative 1/2 of a gaussian kernel (or a normal gaussian kernel)
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

// to generate a sobel kernel we perform the external product of: Dx gaussian kernel * gaussian kernel
// check the way the sobel kernel can be created here https://en.wikipedia.org/wiki/Sobel_operator
// it is the outer product of a vector with gauss and a vector with the derivative applied
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

        // same as the one before BUT check the indexes, first i then j.
        for (i in 0 until rows){
            for (j in 0 until cols){
                kernel[i][j] = sobelDx[i] * sobelDy[j]
            }
        }
    }
}
