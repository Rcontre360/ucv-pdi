package org.pdi.core

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.sqrt
import org.pdi.core.kernels.SobelXKernel
import org.pdi.core.kernels.SobelYKernel

enum class BorderDetectionType {
    SOBEL,
    ROBERTS,
    PREWITT
}

class BorderDetection(val kernelX:Kernel,val kernelY:Kernel) {
    fun apply(image: Image): Image {
        val imageX = image.applyKernel(kernelX)
        val imageY = image.applyKernel(kernelY)

        val width = image.metadata.width
        val height = image.metadata.height
        val newImage = BufferedImage(width, height, image.image.type)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val cx = Color(imageX.image.getRGB(x, y))
                val cy = Color(imageY.image.getRGB(x, y))

                val r = sqrt((cx.red*cx.red + cy.red * cy.red).toDouble()).toInt().coerceIn(0, 255)
                val g = sqrt((cx.green * cx.green + cy.green * cy.green).toDouble()).toInt().coerceIn(0, 255)
                val b = sqrt((cx.blue * cx.blue + cy.blue * cy.blue).toDouble()).toInt().coerceIn(0, 255)

                val newRgb = Color(r,g,b).rgb
                newImage.setRGB(x, y, newRgb)
            }
        }

        return Image(newImage)
    }
}
