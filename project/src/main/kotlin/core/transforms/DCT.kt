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
        gray.convertTo(gray, CvType.CV_32F)

        val dctMat = Mat()
        Core.dct(floatGray, dctMat)

        listOf(gray,floatGray).release()
        return dctMat
    }

    override fun applyFilter(mat: Mat, threshold: Double, highPass: Boolean): Mat {
        val dct = toFrequency(mat)
        val mask = createFilter(dct.cols(), dct.rows(), threshold, highPass)

        val filteredDct = Mat()
        Core.multiply(dct, mask, filteredDct)

        val inverseDct = Mat()
        Core.idct(filteredDct, inverseDct)

        val result = Mat()
        inverseDct.convertTo(result, CvType.CV_8U)

        listOf(dct,mask,filteredDct,inverseDct).release()
        return result
    }

    override fun createFilter(rows: Int, cols: Int, threshold: Double, highPass: Boolean): Mat {
        val baseValue = if (highPass) 1.0 else 0.0
        val rectValue = if (highPass) 0.0 else 1.0
        val mask = Mat(rows, cols, CvType.CV_32F, Scalar(baseValue))

        val filterW = (cols * threshold).toInt()
        val filterH = (rows * threshold).toInt()

        if (filterW > 0 && filterH > 0) {
            val roi = mask.submat(Rect(0, 0, filterW, filterH))
            roi.setTo(Scalar(rectValue))
            roi.release()
        }

        return mask
    }

    override fun logMagnitude(dctMat: Mat): Mat {
        val magnitude = Mat()
        // DCT can have negative values; use absolute to avoid log(negative)
        Core.absdiff(dctMat, Scalar.all(0.0), magnitude)

        Core.add(magnitude, Scalar.all(1.0), magnitude)
        Core.log(magnitude, magnitude)

        val result = Mat()
        Core.normalize(magnitude, result, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

        magnitude.release()
        return result
    }
}
