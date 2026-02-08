package org.pdi.core.transforms

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.pdi.core.image.release

class DFT : Transform() {

    override fun applyFilter(mat: Mat, filter: FrequencyFilter): Mat {
        val dft = toFrequency(mat)

        // El espectro se centra antes de aplicar la máscara circular
        shiftQuadrants(dft)

        val filteredDft = filter.apply(dft)

        // Deshacer el shift para volver al formato nativo de OpenCV antes de la IDFT
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

    override fun logMagnitude(complexImage: Mat): Mat {
        val planes = mutableListOf<Mat>()
        Core.split(complexImage, planes)

        val magnitude = Mat()
        Core.magnitude(planes[0], planes[1], magnitude)

        Core.add(magnitude, Scalar.all(1.0), magnitude)
        Core.log(magnitude, magnitude)

        // Shift para centrar el componente DC en la visualización
        shiftQuadrants(magnitude)

        val result = Mat()
        Core.normalize(magnitude, result, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

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

        listOf(tmp, q0, q1, q2, q3).release()
        return mat
    }
}