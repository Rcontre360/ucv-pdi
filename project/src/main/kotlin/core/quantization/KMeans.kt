package org.pdi.core.quantization

import org.opencv.core.*
import org.pdi.core.image.Image

class KMeansQuantizer(k: Int) : Quantizer(k) {
    override fun apply(image: Image): Mat {
        val mat = image.image
        val samples = mat.reshape(0, mat.rows() * mat.cols())
        samples.convertTo(samples, CvType.CV_32F)

        val criteria = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0)
        val labels = Mat()
        val centers = Mat()

        Core.kmeans(samples, value, labels, criteria, 3, Core.KMEANS_PP_CENTERS, centers)
        centers.convertTo(centers, CvType.CV_8U)

        val result = Mat(mat.rows(), mat.cols(), CvType.CV_8UC3)
        for (i in 0 until mat.rows() * mat.cols()) {
            val label = labels.get(i, 0)[0].toInt()
            val color = ByteArray(3)
            centers.get(label, 0, color)

            result.put(i / mat.cols(), i % mat.cols(),
                (color[0].toInt() and 0xFF).toDouble(),
                (color[1].toInt() and 0xFF).toDouble(),
                (color[2].toInt() and 0xFF).toDouble()
            )
        }

        listOf(samples, labels, centers).forEach { it.release() }
        return result
    }
}