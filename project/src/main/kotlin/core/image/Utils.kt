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
    if (rawColor.size == 1)
        return Color(rawColor[0].toInt(),rawColor[0].toInt(),rawColor[0].toInt())
    else
        return Color(rawColor[2].toInt(),rawColor[1].toInt(),rawColor[0].toInt())
}

fun Mat.putRGB(x:Int,y:Int,c:Color) {
    val bgrBytes = byteArrayOf(c.blue.toByte(), c.green.toByte(), c.red.toByte())
    this.put(y,x, bgrBytes)
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
