package org.pdi.core.transforms

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class DFT : Transform {
    override fun generateFrequencyMat(mat: Mat): Mat {
        val complexImage = performDFT(mat)
        val magnitude = getDFTLogMagnitude(complexImage)
        complexImage.release()
        return magnitude
    }

    override fun apply(mat: Mat, threshold: Double, highPass: Boolean): Mat {
        return applyFrequencyFilter(mat, threshold, highPass)
    }

    override fun createFilter(rows: Int, cols: Int, threshold: Double, highPass: Boolean): Mat {
        return createFilterMask(cols, rows, threshold, highPass)
    }

    private fun performDFT(image: Mat): Mat {
        // we always have 3 channel images even if they are gray
        val gray = Mat()
        if (image.channels() == 1) {
            image.copyTo(gray)
        } else {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
        }

        val planes = listOf(gray, Mat.zeros(gray.size(), CvType.CV_32F))
        gray.convertTo(planes[0], CvType.CV_32F)

        val complexImage = Mat()
        Core.merge(planes, complexImage)
        Core.dft(complexImage, complexImage)

        gray.release()

        return complexImage
    }

    private fun getDFTLogMagnitude(complexImage: Mat): Mat {
        val planes = mutableListOf<Mat>()
        Core.split(complexImage, planes)
        val magnitude = Mat()
        Core.magnitude(planes[0], planes[1], magnitude)

        Core.add(magnitude, Scalar.all(1.0), magnitude)
        Core.log(magnitude, magnitude)

        val crop = magnitude.submat(Rect(0, 0, magnitude.cols() and -2, magnitude.rows() and -2))
        shiftQuadrants(crop)

        val result = Mat()
        Core.normalize(crop, result, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

        magnitude.release()
        planes.forEach { it.release() }

        return result
    }

    private fun shiftQuadrants(mat: Mat): Mat {
        val cx = mat.cols() / 2
        val cy = mat.rows() / 2
        val tmp = Mat()

        val q0 = mat.submat(0, cy, 0, cx)
        val q3 = mat.submat(cy, mat.rows(), cx, mat.cols())
        q0.copyTo(tmp); q3.copyTo(q0); tmp.copyTo(q3)

        val q1 = mat.submat(0, cy, cx, mat.cols())
        val q2 = mat.submat(cy, mat.rows(), 0, cx)
        q1.copyTo(tmp); q2.copyTo(q1); tmp.copyTo(q2)

        tmp.release()
        q0.release()
        q1.release()
        q2.release()
        q3.release()
        return mat
    }

    private fun applyFrequencyFilter(
        source: Mat,
        threshold: Double,
        inverted: Boolean
    ): Mat {
        val dft = performDFT(source)
        shiftQuadrants(dft)

        val filter = createFilterMask(dft.cols(), dft.rows(), threshold, inverted)
        val filterPlanes = listOf(filter, filter.clone())
        val filterComplex = Mat()
        Core.merge(filterPlanes, filterComplex)

        val filteredDft = Mat()
        Core.multiply(dft, filterComplex, filteredDft)
        shiftQuadrants(filteredDft)

        val inverseDft = Mat()
        Core.idft(filteredDft, inverseDft, Core.DFT_REAL_OUTPUT + Core.DFT_SCALE)

        val result = Mat()
        inverseDft.convertTo(result, CvType.CV_8U)

        dft.release()
        filter.release()
        filterComplex.release()
        filteredDft.release()
        inverseDft.release()
        filterPlanes.forEach { it.release() }

        return result
    }

    private fun createFilterMask(width: Int, height: Int, threshold: Double, inverted: Boolean = false): Mat {
        val baseValue = if (inverted) 1.0 else 0.0
        val rectValue = if (inverted) 0.0 else 1.0
        val mask = Mat(height, width, CvType.CV_32F, Scalar(baseValue))

        val filterW = (width * threshold).toInt()
        val filterH = (height * threshold).toInt()
        val x = (width - filterW) / 2
        val y = (height - filterH) / 2

        if (filterW > 0 && filterH > 0) {
            val roi = mask.submat(Rect(x, y, filterW, filterH))
            roi.setTo(Scalar(rectValue))
            roi.release()
        }

        return mask
    }
}
