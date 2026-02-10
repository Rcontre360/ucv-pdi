package org.pdi.core.image

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

fun rgbToYuv(image: Mat): List<Mat> {
    val yuvMat = Mat()
    Imgproc.cvtColor(image, yuvMat, Imgproc.COLOR_BGR2YUV)
    val channels = arrayListOf<Mat>()
    Core.split(yuvMat, channels)
    yuvMat.release()
    return channels
}

fun yuvToRGB(channels: List<Mat>): Mat {
    val yuvMat = Mat()
    Core.merge(channels, yuvMat)
    val bgrMat = Mat()
    Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2BGR)
    yuvMat.release()
    return bgrMat
}

fun rotateHue(hueChannel: Mat, shift: Int): Mat {
    val shifted = Mat()
    Core.add(hueChannel, Scalar(shift.toDouble()), shifted)

    val overMask = Mat()
    Core.compare(shifted, Scalar(179.0), overMask, Core.CMP_GT)
    Core.subtract(shifted, Scalar(180.0), shifted, overMask)

    val underMask = Mat()
    Core.compare(shifted, Scalar(0.0), underMask, Core.CMP_LT)
    Core.add(shifted, Scalar(180.0), shifted, underMask)

    overMask.release()
    underMask.release()
    return shifted
}