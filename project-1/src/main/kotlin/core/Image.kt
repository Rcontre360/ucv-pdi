package org.pdi.core

import kotlin.math.pow
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.Int
import kotlin.math.roundToInt

enum class ZoomAlgorithm {
    CLOSEST_NEIGHTBOUR,
    LINEAR_INTERPOLATION,
}

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
    val isGrayscale = getIsGrayscale()

    fun getMetadata(): Metadata{
        return _metadata
    }

    fun pow(pow: Float): Image {
        return applyPerPixel { i, j, color ->
            if (i == 0 && j == 0)
                println("POW (${i},${j})^${pow} = (${color.red})^${pow} = ${(color.red).toFloat().pow(pow)}")
            Color(
                ((color.red).toFloat().pow(pow)).toInt().coerceIn(0, 255),
                ((color.green).toFloat().pow(pow)).toInt().coerceIn(0, 255),
                ((color.blue).toFloat().pow(pow)).toInt().coerceIn(0, 255),
            ).rgb
        }
    }

    operator fun plus(other: Image): Image {
        if (_metadata.width != other._metadata.width || _metadata.height != other._metadata.height)
            throw IllegalArgumentException("different image sizes for plus operator")

        return applyPerPixel { x, y, color ->
            val other = Color(other.image.getRGB(x, y))
            if (x == 0 && y == 0)
                println("SUM (${x},${y}) = (${color.red}) + (${other.red}) = ${color.red + other.red}")
            Color(
                (color.red + other.red).toInt().coerceIn(0, 255),
                (color.green + other.green).toInt().coerceIn(0, 255),
                (color.blue + other.blue).toInt().coerceIn(0, 255),
            ).rgb
        }
    }

    private fun getIsGrayscale(): Boolean{
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

    fun applyKernel(kernel:Kernel):Image{
        val newImage = BufferedImage(_metadata.width, _metadata.height, BufferedImage.TYPE_INT_RGB)

        for (y in 0 until _metadata.height) {
            for (x in 0 until _metadata.width) {
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

                val r = kernel.convolute(imgR, true).roundToInt().coerceIn(0, 255)
                val g = kernel.convolute(imgG, true).roundToInt().coerceIn(0, 255)
                val b = kernel.convolute(imgB, true).roundToInt().coerceIn(0, 255)

                newImage.setRGB(x,y, Color(r,g,b).rgb)
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
        val sortedUmbrals = umbrals.sorted()

        fun thresholding(v: Int): Int {
            if (sortedUmbrals.isEmpty()) return v

            for (i in sortedUmbrals.indices) {
                if (v < sortedUmbrals[i]) {
                    val intensity = (255.0 / (sortedUmbrals.size) * i).toInt()
                    return intensity.coerceIn(0, 255)
                }
            }
            return 255
        }

        return applyPerPixel { _, _, color ->
            // Since it's a grayscale image, R, G, and B are the same.
            val gray = color.red
            val newGray = thresholding(gray)
            Color(newGray, newGray, newGray).rgb
        }
    }

    fun rotateStraight(angle: Int): Image {
        if (angle % 90 != 0) return this

        val w = _metadata.width
        val h = _metadata.height

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
        val w = (_metadata.width * factor).toInt()
        val h = (_metadata.height * factor).toInt()
        val newImg = BufferedImage(w, h, image.type)

        for (y in 0 until h) {
            for (x in 0 until w) {

                when (algo) {
                    ZoomAlgorithm.CLOSEST_NEIGHTBOUR -> {
                        // when factor < 0 it acts as a multiplicationn. Sometimes it gets out of range
                        val srcX = (x / factor).toInt().coerceIn(0,_metadata.width - 1)
                        val srcY = (y / factor).toInt().coerceIn(0,_metadata.height - 1)
                        val pixel = image.getRGB(srcX, srcY)
                        newImg.setRGB(x, y, pixel)
                    }
                    ZoomAlgorithm.LINEAR_INTERPOLATION -> {
                        val a = (x/factor) - (x / factor).toInt()
                        val b = (y/factor) - (y / factor).toInt()
                        val srcX = (x / factor).toInt().coerceIn(0,_metadata.width - 2)
                        val srcY = (y / factor).toInt().coerceIn(0,_metadata.height - 2)

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

    fun getLineProfile(axis: Char, lineNumber: Int, channel: Char): List<Pair<Int, Int>> {
        val profile = mutableListOf<Pair<Int, Int>>()
        when (axis) {
            'X' -> { // Horizontal line (row)
                if (lineNumber !in 0 until _metadata.height) return emptyList()
                for (x in 0 until _metadata.width) {
                    val color = Color(image.getRGB(x, lineNumber))
                    val value = when (channel) {
                        'R' -> color.red
                        'G' -> color.green
                        'B' -> color.blue
                        'G' -> (color.red + color.green + color.blue) / 3 // Grayscale approximation
                        else -> 0
                    }
                    profile.add(x to value)
                }
            }
            'Y' -> { // Vertical line (column)
                if (lineNumber !in 0 until _metadata.width) return emptyList()
                for (y in 0 until _metadata.height) {
                    val color = Color(image.getRGB(lineNumber, y))
                    val value = when (channel) {
                        'R' -> color.red
                        'G' -> color.green
                        'B' -> color.blue
                        'G' -> (color.red + color.green + color.blue) / 3 // Grayscale approximation
                        else -> 0
                    }
                    profile.add(y to value)
                }
            }
        }
        return profile
    }
}
