package org.pdi.core

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.Int
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class ZoomAlgorithm {
    CLOSEST_NEIGHTBOUR,
    LINEAR_INTERPOLATION,
}

typealias PixelProcessor<T> = (x: Int, y: Int, originalColor: Color) -> T

data class Metadata(
    val width: Int,
    val height: Int,
    val bitsPerPixel: Int,
    val uniqueColors: Int,
    val format: String = "Unknown"
)

class Image(val buff: BufferedImage) {
    val image: BufferedImage = buff
    val metadata = Metadata(
        width = image.width,
        height = image.height,
        bitsPerPixel = image.colorModel.pixelSize,
        uniqueColors = image.colorModel.numComponents
    )

    val histogram: Histogram by lazy { Histogram(calculateHistogram()) }
    val isGrayscale = getIsGrayscale()
    val isBinary = getIsBinary()

    fun readAllPixels(processor: PixelProcessor<Any>) {
        for (y in 0 until metadata.height) {
            for (x in 0 until metadata.width) {
                val pixel = image.getRGB(x, y)
                val color = Color(pixel)
                processor(x, y, color)
            }
        }
    }

    fun getTonalCurve(resImg: Image): Map<Char, IntArray> {
        val sums = Array(4) { LongArray(256) { 0L } } // 0:R, 1:G, 2:B, 3:L
        val counts = Array(4) { IntArray(256) { 0 } }

        for (y in 0 until metadata.height) {
            for (x in 0 until metadata.width) {
                val srcColor = Color(image.getRGB(x, y))
                val dstColor = Color(resImg.image.getRGB(x, y))

                // R, G, B channels
                sums[0][srcColor.red] += dstColor.red
                counts[0][srcColor.red]++
                sums[1][srcColor.green] += dstColor.green
                counts[1][srcColor.green]++
                sums[2][srcColor.blue] += dstColor.blue
                counts[2][srcColor.blue]++

                // Luminosity channel
                val srcLum = (0.2126 * srcColor.red + 0.7152 * srcColor.green + 0.0722 * srcColor.blue).roundToInt()
                val dstLum = (0.2126 * dstColor.red + 0.7152 * dstColor.green + 0.0722 * dstColor.blue).roundToInt()
                sums[3][srcLum] += dstLum
                counts[3][srcLum]++
            }
        }

        val luts = Array(4) { IntArray(256) { -1 } }
        for (c in 0..3) {
            for (i in 0..255) {
                if (counts[c][i] > 0) {
                    luts[c][i] = (sums[c][i] / counts[c][i]).toInt()
                }
            }
            interpolate(luts[c])
        }

        return mapOf(
            'R' to luts[0],
            'G' to luts[1],
            'B' to luts[2],
            'L' to luts[3]
        )
    }

    private fun interpolate(lut: IntArray) {
        var i = 0
        while (i < lut.size) {
            if (lut[i] == -1) {
                val prevX = i - 1
                var nextX = i + 1
                while (nextX < lut.size && lut[nextX] == -1) {
                    nextX++
                }

                if (prevX < 0) { // Gap at the beginning
                    val nextY = if (nextX < lut.size) lut[nextX] else i
                    for (j in i until nextX) {
                        lut[j] = nextY
                    }
                    i = nextX
                } else if (nextX >= lut.size) { // Gap at the end
                    val prevY = lut[prevX]
                    for (j in i until lut.size) {
                        lut[j] = prevY
                    }
                    i = lut.size
                } else { // Gap in the middle
                    val prevY = lut[prevX]
                    val nextY = lut[nextX]
                    for (j in i until nextX) {
                        val t = (j - prevX).toFloat() / (nextX - prevX)
                        lut[j] = (prevY * (1 - t) + nextY * t).roundToInt()
                    }
                    i = nextX
                }
            } else {
                i++
            }
        }
    }

    fun applyKernel(kernel:Kernel):Image{
        return applyPerPixel { x, y, color ->
            val imgR = Array(kernel.rows) {FloatArray(kernel.cols)}
            val imgG = Array(kernel.rows) {FloatArray(kernel.cols)}
            val imgB = Array(kernel.rows) {FloatArray(kernel.cols)}

            for (i in 0 until kernel.rows) {
                for (j in 0 until kernel.cols) {
                    val imageX = (x - kernel.cols / 2 + j).coerceIn(0, image.width - 1)
                    val imageY = (y - kernel.rows / 2 + i).coerceIn(0, image.height - 1)
                    val color = Color(image.getRGB(imageX, imageY))

                    imgR[i][j] = color.red.toFloat()
                    imgG[i][j] = color.green.toFloat()
                    imgB[i][j] = color.blue.toFloat()
                }
            }

            val r = kernel.convolute(imgR).roundToInt().coerceIn(0, 255)
            val g = kernel.convolute(imgG).roundToInt().coerceIn(0, 255)
            val b = kernel.convolute(imgB).roundToInt().coerceIn(0, 255)

            Color(r,g,b).rgb
        }
    }

    fun applyBorderOperator(kernelX:Kernel,kernelY:Kernel):Image{
        val imageX = applyKernel(kernelX)
        val imageY = applyKernel(kernelY)

        return applyPerPixel { x, y, color ->
            val cx = Color(imageX.image.getRGB(x, y))
            val cy = Color(imageY.image.getRGB(x, y))

            val r = sqrt((cx.red*cx.red + cy.red * cy.red).toDouble()).toInt().coerceIn(0, 255)
            val g = sqrt((cx.green * cx.green + cy.green * cy.green).toDouble()).toInt().coerceIn(0, 255)
            val b = sqrt((cx.blue * cx.blue + cy.blue * cy.blue).toDouble()).toInt().coerceIn(0, 255)

            Color(r,g,b).rgb
        }
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
        val sortedUmbrals = umbrals.sorted()

        fun thresholding(v: Int): Int {
            if (sortedUmbrals.isEmpty()) return v

            for (i in sortedUmbrals.indices) {
                if (v < sortedUmbrals[i]) {
                    val intensity = if (i % 2 == 0) {0} else {255}
                    return intensity
                }
            }
            return 255
        }

        return applyPerPixel { _, _, color ->
            val gray = color.red
            val newGray = thresholding(gray)
            Color(newGray, newGray, newGray).rgb
        }
    }

    fun rotateStraight(angle: Int): Image {
        if (angle % 90 != 0) return this

        val w = metadata.width
        val h = metadata.height

        val newWidth: Int
        val newHeight: Int

        when (angle) {
            90, 270 -> {
                newWidth = h
                newHeight = w
            }
            180 -> {
                newWidth = w
                newHeight = h
            }
            else -> return this
        }

        val newImage = BufferedImage(newWidth, newHeight, image.type)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = image.getRGB(x, y)
                when (angle) {
                    90 -> newImage.setRGB(h - 1 - y, x, pixel)
                    180 -> newImage.setRGB(w - 1 - x, h - 1 - y, pixel)
                    270 -> newImage.setRGB(y, w - 1 - x, pixel)
                }
            }
        }

        return Image(newImage)
    }

    fun zoom(factor: Float, algo: ZoomAlgorithm): Image {
        val w = (metadata.width * factor).toInt()
        val h = (metadata.height * factor).toInt()
        val newImg = BufferedImage(w, h, image.type)

        for (y in 0 until h) {
            for (x in 0 until w) {

                when (algo) {
                    ZoomAlgorithm.CLOSEST_NEIGHTBOUR -> {
                        // when factor < 0 it acts as a multiplicationn. Sometimes it gets out of range
                        val srcX = (x / factor).toInt().coerceIn(0,metadata.width - 1)
                        val srcY = (y / factor).toInt().coerceIn(0,metadata.height - 1)
                        val pixel = image.getRGB(srcX, srcY)
                        newImg.setRGB(x, y, pixel)
                    }
                    ZoomAlgorithm.LINEAR_INTERPOLATION -> {
                        val a = (x/factor) - (x / factor).toInt()
                        val b = (y/factor) - (y / factor).toInt()
                        val srcX = (x / factor).toInt().coerceIn(0,metadata.width - 2)
                        val srcY = (y / factor).toInt().coerceIn(0,metadata.height - 2)

                        fun interpolateChannel(is_v:Int, ds:Int, ir:Int, dr:Int): Int{
                            return ((1-a) * (1-b) * is_v + a*(1-b) * ds + (1-a)*b*ir + a*b*dr).toInt();
                        }

                        val p_is = Color(image.getRGB(srcX, srcY))
                        val p_ds =Color( image.getRGB(srcX + 1, srcY))
                        val p_ir = Color(image.getRGB(srcX, srcY + 1))
                        val p_dr = Color(image.getRGB(srcX + 1, srcY + 1))

                        val res = Color(
                            interpolateChannel(p_is.red, p_ds.red, p_ir.red, p_dr.red),
                            interpolateChannel(p_is.green, p_ds.green, p_ir.green, p_dr.green),
                            interpolateChannel(p_is.blue, p_ds.blue, p_ir.blue, p_dr.blue),
                        )
                        newImg.setRGB(x, y, res.rgb)
                    }
                }
            }
        }

        return Image(newImg)
    }

    private fun calculateHistogram(): Map<Int, IntArray> {
        val histogram = mutableMapOf<Int, IntArray>()
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)

        for (y in 0 until metadata.height) {
            for (x in 0 until metadata.width) {
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

    fun getLineProfile(axis: Char, lineNumber: Int, channel: Char): List<Pair<Int, Int>> {
        val profile = mutableListOf<Pair<Int, Int>>()
        when (axis) {
            'X' -> { // Horizontal line (row)
                if (lineNumber !in 0 until metadata.height) return emptyList()
                for (x in 0 until metadata.width) {
                    val color = Color(image.getRGB(x, lineNumber))
                    val value = when (channel) {
                        'R' -> color.red
                        'G' -> color.green
                        'B' -> color.blue
                        'L' -> (color.red + color.green + color.blue) / 3 // Grayscale approximation
                        else -> 0
                    }
                    profile.add(x to value)
                }
            }
            'Y' -> { // Vertical line (column)
                if (lineNumber !in 0 until metadata.width) return emptyList()
                for (y in 0 until metadata.height) {
                    val color = Color(image.getRGB(lineNumber, y))
                    val value = when (channel) {
                        'R' -> color.red
                        'G' -> color.green
                        'B' -> color.blue
                        'L' -> (color.red + color.green + color.blue) / 3 // Grayscale approximation
                        else -> 0
                    }
                    profile.add(y to value)
                }
            }
        }
        return profile
    }

    private fun getIsGrayscale(): Boolean{
        for (y in 0 until metadata.height) {
            for (x in 0 until metadata.width) {
                val pixel = image.getRGB(x, y)
                val color = Color(pixel)
                if (color.red != color.green && color.green != color.blue) {
                    return false
                }
            }
        }
        return true
    }

    private fun getIsBinary(): Boolean{
        val bin = listOf(0,255)
        for (y in 0 until metadata.height) {
            for (x in 0 until metadata.width) {
                val pixel = image.getRGB(x, y)
                val color = Color(pixel)
                if (!(color.red in bin) || !(color.green in bin) || !(color.blue in bin)) {
                    return false
                }
            }
        }
        return true
    }

    private fun applyPerPixel(processor: PixelProcessor<Int>): Image {
        val newImage = BufferedImage(metadata.width, metadata.height, BufferedImage.TYPE_INT_RGB)

        readAllPixels {x,y,color ->
            val pixel = image.getRGB(x, y)
            val color = Color(pixel)
            val newRgbValue = processor(x, y, color)
            newImage.setRGB(x, y, newRgbValue)
        }

        return Image(newImage)
    }

}
