package org.pdi.core.kernels

import org.pdi.core.LinearKernel

//implementation of laplacian taken from: https://github.com/opencv/opencv/blob/4.x/modules/ts/src/ts_func.cpp
//Decided to use this implementation because I dont find a good practice to just hardcode kernels. Also was hard to come up with
// a method that created kernels that sum 0 and followed the rules behind the laplacian function.
// In case we weren't allowed to use OpenCV code, my justification is that I could just have used this code to generate kernels up to 7x7 and hardcode them,
// find better to implement this and show the source where I took it from.

// OTHER SOLUTIONS I tried where:
// 1 - creating gaussian kernel and apply Dxx/Dyy kernels over it and sum the result. This works only for the 3x3 kernel
// 2 - creating an identity I kernel and apply Dxx/Dyy and sum the result. The resulting kernels didnt sum 0 (except for 3x3). Same results as point 1

//This is the best way to dynamically create laplacian kernels I found
//It also creates the same kernels found on other sources. All kernels generated also SUM 0 as expected
// we generate a gauss array (using pascal) G. Then its second derivative DxxG also as an array
// finally we do external product of G and DxxG
class LaplacianKernel(size:Int) : LinearKernel(size, size) {
    override fun isCustomizable():Pair<Boolean,Boolean> = Pair(true,false)

    override fun generateKernel() {
        val gauss = if (cols==3){
            floatArrayOf(0f,1f,0f)
        } else {
            generateSobel1D(0,cols)
        }
        val gaussDx2 = generateSobel1D(2,cols)

        for (i in 0 until rows){
            for (j in 0 until cols){
                val ij = gauss[i] * gaussDx2[j] + gauss[j] * gaussDx2[i]
                kernel[i][j] = ij
            }
        }
    }
}


