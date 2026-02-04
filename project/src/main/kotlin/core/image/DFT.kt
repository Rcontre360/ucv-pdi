package org.pdi.core.image

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc


fun performDFT(image: Mat): Mat {
    val gray = Mat()
    if (image.channels() > 1) {
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
    } else {
        image.copyTo(gray)
    }

    val padded = Mat()
    val optimalRows = Core.getOptimalDFTSize(gray.rows())
    val optimalCols = Core.getOptimalDFTSize(gray.cols())
    Core.copyMakeBorder(gray, padded, 0, optimalRows - gray.rows(), 0, optimalCols - gray.cols(), Core.BORDER_CONSTANT, Scalar.all(0.0))

    val planes = listOf(padded, Mat.zeros(padded.size(), CvType.CV_32F))
    padded.convertTo(planes[0], CvType.CV_32F)

    val complexImage = Mat()
    Core.merge(planes, complexImage)
    Core.dft(complexImage, complexImage)

    gray.release()
    padded.release()

    return complexImage
}

fun getDFTLogMagnitude(complexImage: Mat): Mat {
    val planes = mutableListOf<Mat>()
    Core.split(complexImage, planes)
    val magnitude = Mat()
    Core.magnitude(planes[0], planes[1], magnitude)

    Core.add(magnitude, Scalar.all(1.0), magnitude)
    Core.log(magnitude, magnitude)

    // Crop the spectrum if it has an odd number of rows or columns
    val crop = magnitude.submat(Rect(0, 0, magnitude.cols() and -2, magnitude.rows() and -2))

    val cx = crop.cols() / 2
    val cy = crop.rows() / 2

    val q0 = crop.submat(Rect(0, 0, cx, cy))
    val q1 = crop.submat(Rect(cx, 0, cx, cy))
    val q2 = crop.submat(Rect(0, cy, cx, cy))
    val q3 = crop.submat(Rect(cx, cy, cx, cy))

    val tmp = Mat()
    q0.copyTo(tmp)
    q3.copyTo(q0)
    tmp.copyTo(q3)
    q1.copyTo(tmp)
    q2.copyTo(q1)
    tmp.copyTo(q2)

    val result = Mat()
    Core.normalize(crop, result, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

    tmp.release()
    magnitude.release()
    planes.forEach { it.release() }

    return result
}

fun shiftQuadrants(image: Mat, inPlace: Boolean = true): Mat {
    val mat = if (inPlace) image else image.clone()

    val cx = mat.cols() / 2
    val cy = mat.rows() / 2

    val q0 = mat.submat(Rect(0, 0, cx, cy))
    val q1 = mat.submat(Rect(cx, 0, cx, cy))
    val q2 = mat.submat(Rect(0, cy, cx, cy))
    val q3 = mat.submat(Rect(cx, cy, cx, cy))

    val tmp = Mat()
    q0.copyTo(tmp)
    q3.copyTo(q0)
    tmp.copyTo(q3)
    q1.copyTo(tmp)
    q2.copyTo(q1)
    tmp.copyTo(q2)

    tmp.release()

    return mat
}

fun applyFrequencyFilter(
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

    // Centralized memory cleanup
    dft.release()
    filter.release()
    filterPlanes.forEach { it.release() }
    filterComplex.release()
    filteredDft.release()
    inverseDft.release()

    return result
}