package org.pdi

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class Image(val file: File) {
    val image: BufferedImage = ImageIO.read(file)
    val width: Int = image.width
    val height: Int = image.height
    val bitsPerPixel: Int = image.colorModel.pixelSize
    val uniqueColors: Int = image.colorModel.numComponents
    val histogram: Map<Int, IntArray> = calculateHistogram()

    private fun calculateHistogram(): Map<Int, IntArray> {
        val histogram = mutableMapOf<Int, IntArray>()
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = image.getRGB(x, y)
                red[(color shr 16) and 0xFF]++
                green[(color shr 8) and 0xFF]++
                blue[color and 0xFF]++
            }
        }

        histogram[0] = red
        histogram[1] = green
        histogram[2] = blue

        return histogram
    }
}
