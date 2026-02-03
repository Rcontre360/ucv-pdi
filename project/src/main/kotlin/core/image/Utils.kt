package org.pdi.core.image

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.awt.Color
import kotlin.math.roundToInt

fun Mat.getRGB(x:Int,y:Int): Color {
    val rawColor = this.get(y, x)
    return Color(rawColor[2].toInt(),rawColor[1].toInt(),rawColor[0].toInt())
}

fun Mat.putRGB(x:Int,y:Int,c:Color) {
    this.put(y,x, floatArrayOf(c.blue.toFloat(),c.green.toFloat(),c.red.toFloat()))
}

fun luminosity(c: Color): Int {
    return (0.2126 * c.red + 0.7152 * c.green + 0.0722 * c.blue).roundToInt()
}

fun createFilterMask(width: Int, height: Int, threshold: Double, inverted: Boolean = false): Mat {
    val mask = Mat.zeros(height, width, CvType.CV_32F)
    val centerX = width / 2
    val centerY = height / 2

    // Calculate the dimensions of the rectangular filter based on the threshold
    val filterWidth = (width * threshold).toInt()
    val filterHeight = (height * threshold).toInt()

    val halfFilterWidth = filterWidth / 2
    val halfFilterHeight = filterHeight / 2

    val startX = (centerX - halfFilterWidth).coerceIn(0, width)
    val endX = (centerX + halfFilterWidth).coerceIn(0, width)
    val startY = (centerY - halfFilterHeight).coerceIn(0, height)
    val endY = (centerY + halfFilterHeight).coerceIn(0, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val value = if (x >= startX && x < endX && y >= startY && y < endY) {
                // Inside the central rectangle
                if (!inverted) 1.0 else 0.0
            } else {
                // Outside the central rectangle
                if (!inverted) 0.0 else 1.0
            }
            mask.put(y, x, value)
        }
    }
    return mask
}

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