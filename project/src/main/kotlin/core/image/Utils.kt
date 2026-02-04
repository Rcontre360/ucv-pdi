package org.pdi.core.image

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
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

fun Collection<Mat>.release() {
    this.forEach { mat ->
        if (!mat.empty()) {
            mat.release()
        }
    }
}

fun luminosity(c: Color): Int {
    return (0.2126 * c.red + 0.7152 * c.green + 0.0722 * c.blue).roundToInt()
}

fun applyChannelFactor(channel:Mat, factor:Float):Mat {
    val res = Mat()
    channel.convertTo(res, CvType.CV_32F)

    Core.add(res, Scalar(factor * 255.0), res)
    res.convertTo(res, CvType.CV_8U)
    return res
}

fun calculateRotatedSize(w: Int, h: Int, angle: Int): Size {
    val rad = Math.toRadians(angle.toDouble())
    // Use absolute values to ensure dimensions always represent positive magnitude
    val sin = Math.abs(Math.sin(rad))
    val cos = Math.abs(Math.cos(rad))

    val newW = (w * cos + h * sin)
    val newH = (w * sin + h * cos)

    return Size(newW, newH)
}