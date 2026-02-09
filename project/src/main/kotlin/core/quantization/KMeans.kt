package org.pdi.core.quantization

import org.opencv.core.*
import org.pdi.core.image.Image

class KMeansQuantizer(k: Int) : Quantizer(k) {
    override fun apply(image: Image): Mat {
        val mat = image.image
        val rows = mat.rows()
        val cols = mat.cols()

        val samples = mat.reshape(1, rows * cols)
        samples.convertTo(samples, CvType.CV_32F)

        val stopRule = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0)
        val labels = Mat()
        val centers = Mat()

        Core.kmeans(samples, value, labels, stopRule, 3, Core.KMEANS_PP_CENTERS, centers)

        centers.convertTo(centers, CvType.CV_8U)

        val quanticized = Mat(rows * cols, 1, CvType.CV_8UC3)

        for (i in 0 until value) {
            val mask = Mat()
            Core.compare(labels, Scalar(i.toDouble()), mask, Core.CMP_EQ)

            val colorData = ByteArray(3)
            centers.get(i, 0, colorData)
            val colorScalar = Scalar(
                (colorData[0].toInt() and 0xFF).toDouble(),
                (colorData[1].toInt() and 0xFF).toDouble(),
                (colorData[2].toInt() and 0xFF).toDouble()
            )

            quanticized.setTo(colorScalar, mask)

            mask.release()
        }

        val result = quanticized.reshape(3, rows)
        listOf(samples, labels, centers, quanticized).forEach { it.release() }

        return result
    }
}