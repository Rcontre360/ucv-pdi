package org.pdi.core

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.Int

typealias PixelProcessor = (x: Int, y: Int, originalColor: Color) -> Int

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
    val histogram: Histogram by lazy { Histogram(calculateHistogram()) }
    val is_grayscale = isGrayscale()

    fun getMetadata(): Metadata{
        return _metadata
    }

    private fun isGrayscale(): Boolean{
        for (y in 0 until _metadata.height) {
            for (x in 0 until _metadata.width) {
                val pixel = image.getRGB(x, y)
                val color = Color(pixel)
                if (color.red != color.green && color.green != color.blue) {
                    return false
                }
            }
        }
        return true
    }

    private fun applyPerPixel(processor: PixelProcessor): Image {
        val newImage = BufferedImage(_metadata.width, _metadata.height, BufferedImage.TYPE_INT_RGB)

        for (y in 0 until _metadata.height) {
            for (x in 0 until _metadata.width) {
                val pixel = image.getRGB(x, y)
                val color = Color(pixel)
                val newRgbValue = processor(x, y, color)
                newImage.setRGB(x, y, newRgbValue)
            }
        }

        return Image(newImage)
    }

    fun tonalCurve(resImg: Image): List<Pair<Color,Color>>{
        val res: MutableList<Pair<Color, Color>> = mutableListOf()

        for (y in 0 until _metadata.height) {
            for (x in 0 until _metadata.width) {
                val src = Color(image.getRGB(x, y))
                val dst = Color(resImg.image.getRGB(x, y))
                res.add(src to dst)
            }
        }

        return res
    }

    fun toGrayscale(tint: Color): Image {
        val (tintR, tintG, tintB) = listOf(tint.red / 255.0, tint.green / 255.0, tint.blue / 255.0)

        return applyPerPixel { _, _, color ->
            val gray = (0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue).toInt().coerceIn(0, 255)

            Color(
                (gray * tintR).toInt().coerceIn(0, 255),
                (gray * tintG).toInt().coerceIn(0, 255),
                (gray * tintB).toInt().coerceIn(0, 255)
            ).rgb
        }
    }

    fun negative(): Image {
        return applyPerPixel { _, _, color ->
            Color(
                255 - color.red,
                255 - color.green,
                255 - color.blue
            ).rgb
        }
    }

    fun changeBrightness(factor: Float): Image {
        return applyPerPixel { _, _, color ->
            Color(
                (color.red * (1 + factor)).toInt().coerceIn(0, 255),
                (color.green * (1 + factor)).toInt().coerceIn(0, 255),
                (color.blue * (1 + factor)).toInt().coerceIn(0, 255)
            ).rgb
        }
    }

    fun changeContrast(factor: Float): Image {
        val min = (factor * 127)
        val max = 255 - min
        println(min)
        println(max)
        val newHistogram = histogram.stretch(min.toInt(),max.toInt())

        return applyPerPixel { _, _, color ->
            val rMap = newHistogram[0]!!
            val gMap = newHistogram[1]!!
            val bMap = newHistogram[2]!!

            val newR = rMap[color.red]
            val newG = gMap[color.green]
            val newB = bMap[color.blue]

            Color(newR, newG, newB, color.alpha).rgb
        }
    }

    fun makeThreshold(umbrals: Array<Int>): Image {
        fun thresholding(v: Int): Int{
            for (i in 1..umbrals.size){
                if (v > umbrals[i-1] && v < umbrals[i]){
                    return (255 / umbrals.size * i).toInt()
                }
            }
            return 255
        }

        return applyPerPixel { _, _, color ->
            Color(
                thresholding(color.red),
                thresholding(color.green),
                thresholding(color.blue)
            ).rgb
        }
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