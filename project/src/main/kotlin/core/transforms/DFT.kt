package org.pdi.core.transforms

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.pdi.core.image.getRGB
import org.pdi.core.image.release

class DFT : Transform() {

    override fun applyFilter(mat: Mat, filter: FrequencyFilter): Mat {
        val dft = toFrequency(mat)

        shiftQuadrants(dft)
        val filteredDft = filter.apply(dft)
        shiftQuadrants(filteredDft)

        val inverseDft = Mat()
        Core.idft(filteredDft, inverseDft, Core.DFT_REAL_OUTPUT + Core.DFT_SCALE)

        val result = Mat()
        inverseDft.convertTo(result, CvType.CV_8U)

        listOf(dft, filteredDft, inverseDft).release()

        return result
    }

    override fun createFilter(rows: Int, cols: Int, threshold: Double, highPass: Boolean): FrequencyFilter {
        return if (highPass) {
            HighPassDFT(rows, cols, threshold)
        } else {
            LowPassDFT(rows, cols, threshold)
        }
    }

    override fun toFrequency(image: Mat): Mat {
        val gray = Mat()
        if (image.channels() == 1) {
            image.copyTo(gray)
        } else {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
        }

        val floatGray = Mat()
        gray.convertTo(floatGray, CvType.CV_32F)

        val complexImage = Mat()
        val planes = listOf(floatGray, Mat.zeros(gray.size(), CvType.CV_32F))

        Core.merge(planes, complexImage)
        Core.dft(complexImage, complexImage)

        gray.release()
        planes.release()

        return complexImage
    }

    override fun logMagnitude(cImage: Mat): Mat {
        val planes = mutableListOf<Mat>()
        Core.split(cImage, planes)

        val mag = Mat()
        Core.magnitude(planes[0], planes[1], mag)
        Core.add(mag, Scalar(1.0), mag)
        Core.log(mag, mag)
        shiftQuadrants(mag)

        val res = Mat()
        Core.normalize(mag, res, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)
        Core.bitwise_not(res, res)

        mag.release()
        planes.forEach { it.release() }
        return res
    }

    fun phase(cImage: Mat): Mat {
        val planes = mutableListOf<Mat>()
        Core.split(cImage, planes)

        val phase = Mat()
        Core.phase(planes[0], planes[1], phase)

        shiftQuadrants(phase)

        val res = Mat()
        Core.normalize(phase, res, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

       phase.release()
        planes.release()

        return res
    }

    fun shiftQuadrants(mat: Mat) {
        val rows = mat.rows()
        val cols = mat.cols()
        val channels = mat.channels()
        val data = FloatArray(rows * cols * channels)
        mat.get(0, 0, data)

        val cx = cols / 2
        val cy = rows / 2

        fun getIdx(x: Int, y: Int): Int = (y * cols + x) * channels

        for (y in 0 until cy) {
            for (x in 0 until cx) {
                val p0 = getIdx(x, y)
                val p1 = getIdx(x + cx, y)
                val p2 = getIdx(x, y + cy)
                val p3 = getIdx(x + cx, y + cy)

                for (c in 0 until channels) {
                    val t1 = data[p0 + c]
                    data[p0 + c] = data[p3 + c]
                    data[p3 + c] = t1

                    val t2 = data[p1 + c]
                    data[p1 + c] = data[p2 + c]
                    data[p2 + c] = t2
                }
            }
        }
        mat.put(0, 0, data)
    }
}