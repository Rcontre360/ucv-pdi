package org.pdi.io

import javafx.scene.image.WritableImage
import javafx.scene.image.PixelFormat
import javafx.scene.image.PixelWriter

import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.CvType
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

private var writableImage: WritableImage? = null
private var buffer: ByteArray? = null

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