package org.pdi.io

import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

fun BufferedImage.toMat(): Mat {
    val byteArrayOutputStream = java.io.ByteArrayOutputStream()
    ImageIO.write(this, "png", byteArrayOutputStream)
    byteArrayOutputStream.flush()
    return Imgcodecs.imdecode(MatOfByte(*byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED)
}

fun Mat.toBufferedImage(): BufferedImage {
    val matOfByte = MatOfByte()
    Imgcodecs.imencode(".png", this, matOfByte)
    val byteArray = matOfByte.toArray()
    val bis = ByteArrayInputStream(byteArray)
    return ImageIO.read(bis)
}
