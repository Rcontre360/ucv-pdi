package org.pdi.core.image

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * Performs the Discrete Cosine Transform.
 * Input should be single channel CV_32F and even-sized.
 */
fun performDCT(image: Mat): Mat {
    val gray = Mat()
    if (image.channels() == 1) {
        image.copyTo(gray)
    } else {
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
    }

    // DCT requires even dimensions. We crop 1 pixel if size is odd.
    val evenRows = gray.rows() and -2
    val evenCols = gray.cols() and -2
    val cropped = gray.submat(Rect(0, 0, evenCols, evenRows))

    val floatGray = Mat()
    cropped.convertTo(floatGray, CvType.CV_32F)

    val dctMat = Mat()
    Core.dct(floatGray, dctMat)

    listOf(gray, cropped, floatGray).release()
    return dctMat
}

/**
 * Normalizes the DCT coefficients for visualization using Log scaling.
 */
fun getDCTLogMagnitude(dctMat: Mat): Mat {
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

/**
 * Applies a frequency filter in the DCT domain.
 * Note: Low frequencies are in the TOP-LEFT in DCT.
 */
fun applyDCTFrequencyFilter(
    source: Mat,
    threshold: Double,
    inverted: Boolean
): Mat {
    val dct = performDCT(source)

    // In DCT, Low Frequencies are top-left, High Frequencies are bottom-right.
    // We create a mask where the top-left corner is the threshold.
    val mask = createDCTFilterMask(dct.cols(), dct.rows(), threshold, inverted)

    val filteredDct = Mat()
    Core.multiply(dct, mask, filteredDct)

    val inverseDct = Mat()
    Core.idct(filteredDct, inverseDct)

    val result = Mat()
    inverseDct.convertTo(result, CvType.CV_8U)

    listOf(dct, mask, filteredDct, inverseDct).release()
    return result
}

/**
 * Creates a mask for DCT. Low frequencies are top-left [0,0].
 */
fun createDCTFilterMask(width: Int, height: Int, threshold: Double, inverted: Boolean): Mat {
    val baseValue = if (inverted) 1.0 else 0.0
    val rectValue = if (inverted) 0.0 else 1.0
    val mask = Mat(height, width, CvType.CV_32F, Scalar(baseValue))

    // Keep the top-left portion (Low Frequencies)
    val filterW = (width * threshold).toInt()
    val filterH = (height * threshold).toInt()

    if (filterW > 0 && filterH > 0) {
        val roi = mask.submat(Rect(0, 0, filterW, filterH))
        roi.setTo(Scalar(rectValue))
        roi.release()
    }

    return mask
}