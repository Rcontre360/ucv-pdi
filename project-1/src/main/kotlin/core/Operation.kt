package org.pdi.core

import java.awt.image.BufferedImage
import kotlin.math.sqrt

abstract class Operation {
    abstract fun apply(image: BufferedImage): BufferedImage
}

class Sobel : Operation() {
    override fun apply(image: BufferedImage): BufferedImage {
        val sobelX = SobelXKernel(3, 3)
        val sobelY = SobelYKernel(3, 3)

        val imageX = sobelX.execute(image)
        val imageY = sobelY.execute(image)

        val width = image.width
        val height = image.height
        val newImage = BufferedImage(width, height, image.type)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val rgbX = imageX.getRGB(x, y)
                val rX = (rgbX shr 16) and 0xFF
                val gX = (rgbX shr 8) and 0xFF
                val bX = rgbX and 0xFF

                val rgbY = imageY.getRGB(x, y)
                val rY = (rgbY shr 16) and 0xFF
                val gY = (rgbY shr 8) and 0xFF
                val bY = rgbY and 0xFF

                val r = sqrt((rX * rX + rY * rY).toDouble()).toInt().coerceIn(0, 255)
                val g = sqrt((gX * gX + gY * gY).toDouble()).toInt().coerceIn(0, 255)
                val b = sqrt((bX * bX + bY * bY).toDouble()).toInt().coerceIn(0, 255)

                val newRgb = (r shl 16) or (g shl 8) or b
                newImage.setRGB(x, y, newRgb)
            }
        }

        return newImage
    }
}
