package org.pdi.core.image

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun Color.euclidean(other: Color): Double {
    val dr = (this.red - other.red).toDouble()
    val dg = (this.green - other.green).toDouble()
    val db = (this.blue - other.blue).toDouble()

    return sqrt(dr * dr + dg * dg + db * db)
}

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

fun Mat.toBufferedImage(): BufferedImage {
    val matOfByte = MatOfByte()
    Imgcodecs.imencode(".png", this, matOfByte)
    val byteArray = matOfByte.toArray()
    val bis = ByteArrayInputStream(byteArray)
    return ImageIO.read(bis)
}

// makes easier to release many Mat
fun Collection<Mat>.release() {
    this.forEach { mat ->
        if (!mat.empty()) {
            mat.release()
        }
    }
}

fun BufferedImage.toMat(): Mat {
    val byteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(this, "png", byteArrayOutputStream)
    byteArrayOutputStream.flush()
    return Imgcodecs.imdecode(MatOfByte(*byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED)
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
    val sin = Math.abs(Math.sin(rad))
    val cos = Math.abs(Math.cos(rad))

    val newW = (w * cos + h * sin)
    val newH = (w * sin + h * cos)

    return Size(newW, newH)
}

