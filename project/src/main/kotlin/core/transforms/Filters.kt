package org.pdi.core.transforms

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

abstract class FrequencyFilter(
    val rows: Int,
    val cols: Int,
    val threshold: Double
) {
    abstract fun generateMask(): Mat
    abstract fun apply(spectralMat: Mat): Mat

    fun getAsImage(): Mat {
        val mask = generateMask()
        val display = Mat()
        mask.convertTo(display, CvType.CV_8U, 255.0)
        mask.release()
        return display
    }

    protected fun invertMask(mask: Mat) {
        val ones = Mat.ones(mask.size(), mask.type())
        Core.subtract(ones, mask, mask)
        ones.release()
    }
}

abstract class DFTFrequencyFilter(r: Int, c: Int, t: Double) : FrequencyFilter(r, c, t) {

    override fun apply(spectralMat: Mat): Mat {
        val mask = generateMask()
        val complexMask = Mat()
        val result = Mat()

        Core.merge(listOf(mask, mask), complexMask)
        Core.multiply(spectralMat, complexMask, result)

        mask.release()
        complexMask.release()
        return result
    }

    protected fun createCircleBase(): Mat {
        val mask = Mat.zeros(Size(cols.toDouble(), rows.toDouble()), CvType.CV_32F)
        val center = Point(cols / 2.0, rows / 2.0)
        val radius = (Math.min(cols, rows) / 2.0) * threshold
        Imgproc.circle(mask, center, radius.toInt(), Scalar(1.0), -1)
        return mask
    }
}

abstract class DCTFrequencyFilter(r: Int, c: Int, t: Double) : FrequencyFilter(r, c, t) {

    override fun apply(spectralMat: Mat): Mat {
        val mask = generateMask()
        val result = Mat()
        Core.multiply(spectralMat, mask, result)
        mask.release()
        return result
    }

    protected fun createRectBase(): Mat {
        val mask = Mat.zeros(Size(cols.toDouble(), rows.toDouble()), CvType.CV_32F)
        val limitX = (cols * threshold).toInt()
        val limitY = (rows * threshold).toInt()
        if (limitX > 0 && limitY > 0) {
            mask.submat(Rect(0, 0, limitX, limitY)).setTo(Scalar(1.0))
        }
        return mask
    }
}

class LowPassDFT(r: Int, c: Int, t: Double) : DFTFrequencyFilter(r, c, t) {
    override fun generateMask() = createCircleBase()
}

class HighPassDFT(r: Int, c: Int, t: Double) : DFTFrequencyFilter(r, c, t) {
    override fun generateMask() = createCircleBase().also { invertMask(it) }
}

class LowPassDCT(r: Int, c: Int, t: Double) : DCTFrequencyFilter(r, c, t) {
    override fun generateMask() = createRectBase()
}

class HighPassDCT(r: Int, c: Int, t: Double) : DCTFrequencyFilter(r, c, t) {
    override fun generateMask() = createRectBase().also { invertMask(it) }
}