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
    println("toBufferedImage")
    val matOfByte = MatOfByte()
    Imgcodecs.imencode(".png", this, matOfByte)
    val byteArray = matOfByte.toArray()
    val bis = ByteArrayInputStream(byteArray)
    return ImageIO.read(bis)
}

fun Mat.toWritableImage(): WritableImage {
    val matToRender: Mat
    val conversionDone: Boolean
    val type = this.type()

    when (type) {
        CvType.CV_8UC1 -> {
            matToRender = Mat()
            Imgproc.cvtColor(this, matToRender, Imgproc.COLOR_GRAY2BGRA)
            conversionDone = true
        }
        CvType.CV_8UC3 -> {
            matToRender = Mat()
            Imgproc.cvtColor(this, matToRender, Imgproc.COLOR_BGR2BGRA)
            conversionDone = true
        }
        CvType.CV_8UC4 -> {
            matToRender = this
            conversionDone = false
        }
        else -> throw UnsupportedOperationException("Unsupported Mat type: $type")
    }

    val width = matToRender.cols()
    val height = matToRender.rows()

    if (writableImage == null || writableImage!!.width.toInt() != width || writableImage!!.height.toInt() != height) {
        writableImage = WritableImage(width, height)
    }

    val pixelWriter: PixelWriter = writableImage!!.pixelWriter
    val bytes = ByteArray(matToRender.total().toInt() * matToRender.elemSize().toInt())
    matToRender.get(0, 0, bytes)

    pixelWriter.setPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(), bytes, 0, matToRender.cols() * matToRender.channels())

    if (conversionDone) {
        matToRender.release()
    }
    return writableImage!!
}

