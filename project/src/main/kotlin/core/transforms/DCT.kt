package org.pdi.core.transforms

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.pdi.core.image.release

class DCT : Transform() {
    override fun toFrequency(mat: Mat): Mat {
        val gray = Mat()
        if (mat.channels() == 1) {
            mat.copyTo(gray)
        } else {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        }

        val floatGray = Mat()
        gray.convertTo(floatGray, CvType.CV_32F)

        val dctMat = Mat()
        Core.dct(floatGray, dctMat)

        listOf(gray, floatGray).release()
        return dctMat
    }

    override fun applyFilter(mat: Mat, filter: FrequencyFilter): Mat {
        val dct = toFrequency(mat)
        val filteredDct = filter.apply(dct)

        val inverseDct = Mat()
        Core.idct(filteredDct, inverseDct)

        val result = Mat()
        inverseDct.convertTo(result, CvType.CV_8U)

        listOf(dct, filteredDct, inverseDct).release()
        return result
    }

    override fun createFilter(rows: Int, cols: Int, threshold: Double, highPass: Boolean): FrequencyFilter {
        return if (highPass) {
            HighPassDCT(rows, cols, threshold)
        } else {
            LowPassDCT(rows, cols, threshold)
        }
    }

    override fun logMagnitude(dctMat: Mat): Mat {
        val mag = Mat()
        Core.absdiff(dctMat, Scalar(0.0), mag)

        Core.add(mag, Scalar(1.0), mag)
        Core.log(mag, mag)

        val res = Mat()
        Core.normalize(mag, res, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)
        Core.bitwise_not(res, res)

        mag.release()
        return res
    }
}