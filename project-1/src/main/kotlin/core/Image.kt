package org.pdi.core

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.Int

data class Metadata(
    val width: Int,
    val height: Int,
    val bitsPerPixel: Int,
    val uniqueColors: Int,
    val format: String = "Unknown"
)

class Image(val buff: BufferedImage) {
    val image: BufferedImage = buff
    val _metadata = Metadata(
        width = image.width,
        height = image.height,
        bitsPerPixel = image.colorModel.pixelSize,
        uniqueColors = image.colorModel.numComponents
    )
    val histogram: Map<Int, IntArray> = calculateHistogram()

    fun getMetadata(): Metadata{
        return _metadata
    }

    fun toGrayscale(tint: Color): Image {
        val newImage = BufferedImage(_metadata.width, _metadata.height, BufferedImage.TYPE_INT_RGB)
        val (tintR,tintG,tintB) = listOf(tint.red / 255.0, tint.green / 255.0, tint.blue / 255.0)

        for (y in 0 until _metadata.height) {
            for (x in 0 until _metadata.width) {
                val pixel = image.getRGB(x, y)
                val color = Color(pixel)
                val gray = (0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue).toInt().coerceIn(0, 255)

                val newColor = Color(
                    (gray * tintR).toInt().coerceIn(0, 255),
                    (gray * tintG).toInt().coerceIn(0, 255),
                    (gray * tintB).toInt().coerceIn(0, 255)
                ).rgb
                newImage.setRGB(x, y, newColor)
            }
        }
        return Image(newImage)
    }

    private fun calculateHistogram(): Map<Int, IntArray> {
        val histogram = mutableMapOf<Int, IntArray>()
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)

        for (y in 0 until _metadata.height) {
            for (x in 0 until _metadata.width) {
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