package org.pdi.core

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.Int
import kotlin.math.roundToInt
import kotlin.math.sqrt

// zoom algorithm type
enum class ZoomAlgorithm {
    CLOSEST_NEIGHTBOUR,
    LINEAR_INTERPOLATION,
}

// callback function for some utilities inside image
typealias PixelProcessor<T> = (x: Int, y: Int, originalColor: Color) -> T

// metadata class. this represent the current image data, even when we zoom the image pixels grow
// the bitsPerPixel is internally always 24 but we represent how many bits we would use
// if the image is STORED
data class Metadata(
    val width: Int,
    val height: Int,
    val bitsPerPixel: Int,
    val uniqueColors: Int,
    val format: String = "Unknown"
)

// image holds an image and applies operations to it. "image" is NEVER modified
// instead of modifying it, we copy it and modify the copy. Each time you modify
// the image you are creating a new copy of it.
// I used this approach since inmutability allows me to assume things about variables
// and have "less" bugs. In the future this should be mutable for more performance
class Image(val buff: BufferedImage) {
    val image: BufferedImage = buff
    // since this image is inmutable all the related variables can be inmutable
    // histogram is only calculated once, same with all the other fields
    val histogram: Histogram by lazy { Histogram(calculateHistogram()) }
    val isGrayscale = getIsGrayscale()
    val isBinary = getIsBinary()
    val metadata = Metadata(
        width = image.width,
        height = image.height,
        uniqueColors = getUniqueColors() ,
        bitsPerPixel = if (isBinary) {1} else{ if (isGrayscale){8} else {24} },
    )

    // runs a function over all pixels, readonly
    fun readAllPixels(processor: PixelProcessor<Any>) {
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.getRGB(x, y)
                val color = Color(pixel)
                processor(x, y, color)
            }
        }
    }

    // returns the tonal curve of the image. The src image is "this" and the target (f(x)) is the "resImg"
    // I decided to make this frequencies based implementation because its more robust than just doing a
    // mapping between one color x to color y. Because for many pixels with color a onn the image they might
    // map to different colors on the dest image. So this handles that use case better.
    fun getTonalCurve(resImg: Image): Map<Char, IntArray> {
        val sums = Array(4) { LongArray(256) { 0L } }
        val counts = Array(4) { IntArray(256) { 0 } }

        for (y in 0 until metadata.height) {
            for (x in 0 until metadata.width) {
                val srcColor = Color(image.getRGB(x, y))
                val dstColor = Color(resImg.image.getRGB(x, y))

                // R, G, B channels
                sums[0][srcColor.red] = sums[0][srcColor.red] + dstColor.red
                counts[0][srcColor.red]++
                sums[1][srcColor.green] = sums[1][srcColor.green] + dstColor.green
                counts[1][srcColor.green]++
                sums[2][srcColor.blue] = sums[2][srcColor.blue] + dstColor.blue
                counts[2][srcColor.blue]++

                // Luminosity channel
                val srcLum = (0.2126 * srcColor.red + 0.7152 * srcColor.green + 0.0722 * srcColor.blue).roundToInt()
                val dstLum = (0.2126 * dstColor.red + 0.7152 * dstColor.green + 0.0722 * dstColor.blue).roundToInt()
                sums[3][srcLum] = sums[3][srcLum] + dstLum
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
            // its VERY important to INTERPOLATE. Because for some images that lack some colors the poitns are missing
            // this will lead to a wrong line graph when displaying it
            interpolate(luts[c])
        }

        return mapOf(
            'R' to luts[0],
            'G' to luts[1],
            'B' to luts[2],
            'L' to luts[3]
        )
    }

    // this function is used to interpolate the LUT table onn the tonal curve. The smaller and more colorful the image,
    // the better the tonal curve will look like.  The less colors it has the more spikes and weird details it will have
    private fun interpolate(lut: IntArray) {
        var i = 0
        while (i < lut.size) {
            if (lut[i] == -1) {
                val prevX = i - 1
                var nextX = i + 1
                // interpolation is to fill gaps, so we run from the last known point to the next known point
                while (nextX < lut.size && lut[nextX] == -1) {
                    nextX++
                }

                // then we interpolate points in the middle based on their distance
                val prevY = if (prevX < 0) {lut[nextX]} else {lut[prevX]}
                val nextY = if (nextX >= lut.size) {lut[prevX]} else {lut[nextX]}
                for (j in i until nextX) {
                    // distance based "t"
                    val t = (j - prevX).toFloat() / (nextX - prevX)
                    // interpolation
                    lut[j] = (prevY * (1 - t) + nextY * t).roundToInt()
                }
                i = nextX
            } else {
                i++
            }
        }
    }

    // applies a kernel to the current image and returns its result
    // this function calculates the window we must send to the kernel and uses the convolute function to get the value
    fun applyKernel(kernel:Kernel):Image{
        // for each pixel we do the following. Calculate the window surrounding that pixel, the size of the kernel
        return applyPerPixel { x, y, _ ->
            // the matrixes that will hold the image windows
            val imgR = Array(kernel.rows) {FloatArray(kernel.cols)}
            val imgG = Array(kernel.rows) {FloatArray(kernel.cols)}
            val imgB = Array(kernel.rows) {FloatArray(kernel.cols)}

            for (i in 0 until kernel.rows) {
                for (j in 0 until kernel.cols) {
                    // we calculate the window index
                    val imageX = (x - kernel.cols / 2 + j)
                    val imageY = (y - kernel.rows / 2 + i)
                    // if the window is OUTSIDE the image. We apply some "padding" with black, this wont mess with borders
                    val color = if (imageX < 0 || imageX >= image.width || imageY < 0 || imageY >= image.height){
                        Color.BLACK
                    } else {
                        Color(image.getRGB(imageX, imageY))
                    }

                    imgR[i][j] = color.red.toFloat()
                    imgG[i][j] = color.green.toFloat()
                    imgB[i][j] = color.blue.toFloat()
                }
            }

            // we convolute with each channel
            val r = kernel.convolute(imgR).roundToInt().coerceIn(0, 255)
            val g = kernel.convolute(imgG).roundToInt().coerceIn(0, 255)
            val b = kernel.convolute(imgB).roundToInt().coerceIn(0, 255)

            Color(r,g,b).rgb
        }
    }

    // this applies a given border operator (we can even mix them since this receives 2 kernels)
    // it applies the kernels and calculates the given gradient
    fun applyBorderOperator(kernelX:Kernel,kernelY:Kernel):Image{
        val imageX = applyKernel(kernelX)
        val imageY = applyKernel(kernelY)

        return applyPerPixel { x, y, _ ->
            // get both colors from both images
            val cx = Color(imageX.image.getRGB(x, y))
            val cy = Color(imageY.image.getRGB(x, y))

            // sqrt( x^2 + y^2 ) onn each channel
            val r = sqrt((cx.red*cx.red + cy.red * cy.red).toDouble()).toInt().coerceIn(0, 255)
            val g = sqrt((cx.green * cx.green + cy.green * cy.green).toDouble()).toInt().coerceIn(0, 255)
            val b = sqrt((cx.blue * cx.blue + cy.blue * cy.blue).toDouble()).toInt().coerceIn(0, 255)

            Color(r,g,b).rgb
        }
    }

    // just a simple grayscale function. With a small trick were we add a tint to make the grayscale
    // if you pick WHITE it will be the same as a normal grayscale
    fun toGrayscale(tint: Color): Image {
        val (tintR, tintG, tintB) = listOf(tint.red / 255.0, tint.green / 255.0, tint.blue / 255.0)

        return applyPerPixel { _, _, color ->
            val gray = luminosity(color)

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

    // simple brightness function. Instead of adding a constant it multiplies by a factor (between 0-2)
    fun changeBrightness(factor: Float): Image {
        return applyPerPixel { _, _, color ->
            Color(
                (color.red * (1 + factor)).toInt().coerceIn(0, 255),
                (color.green * (1 + factor)).toInt().coerceIn(0, 255),
                (color.blue * (1 + factor)).toInt().coerceIn(0, 255)
            ).rgb
        }
    }

    // changes the image contrast. The method used is stretching the histogram
    // to pick the min/max limits we allow the user to do it on will. Since most apps allow the user to change the
    // contrast "level". We use a factor between 0-1. 0.1 would be stretching the histogram from 12.7 to 255-12.7
    fun changeContrast(factor: Float): Image {
        val min = (127 * factor)
        val max = 255 - (127 * factor)
        val newHistogram = histogram.stretch(min.toInt(),max.toInt())

        return applyPerPixel { _, _, color ->
            // we just map using the new histogram
            val rMap = newHistogram[0]!!
            val gMap = newHistogram[1]!!
            val bMap = newHistogram[2]!!

            val newR = rMap[color.red]
            val newG = gMap[color.green]
            val newB = bMap[color.blue]

            Color(newR, newG, newB, color.alpha).rgb
        }
    }

    // makes a thresholding given a set of umbrals. The umbrals is just an array of numbers between 0 and 255,
    // where the numbers dont repeat..or shouldnt :)
    fun makeThreshold(umbrals: Array<Int>): Image {
        val sortedUmbrals = umbrals.sorted()

        fun thresholding(v: Int): Int {
            if (sortedUmbrals.isEmpty()) return v

            // very easy, this just returns the new value of a given pixel
            for (i in sortedUmbrals.indices) {
                // since umbrals are sorted, we will start with the lowest one until finding the right umbral
                if (v < sortedUmbrals[i]) {
                    // which color is this interval? if we split the 0-255 intervals, the first interval obtained will
                    // always be BLACK, the next one obviously needs to be white, since black again is like having a single interval.
                    // we keep doing this until reaching the last interval
                    val intensity = if (i % 2 == 0) {0} else {255}
                    return intensity
                }
            }
            return 255
        }

        // just applying the above function on a single channel and copying that on the rest
        return applyPerPixel { _, _, color ->
            val gray = color.red
            val newGray = thresholding(gray)
            Color(newGray, newGray, newGray).rgb
        }
    }

    // we rotate the image on a straight angle
    fun rotateStraight(_angle: Int): Image {
        var normalizedAngle = _angle % 360
        if (normalizedAngle < 0) {
            normalizedAngle += 360
        }

        if (normalizedAngle % 90 != 0) return this

        val w = metadata.width
        val h = metadata.height

        val newWidth: Int
        val newHeight: Int

        when (normalizedAngle) {
            90, 270 -> {
                newWidth = h
                newHeight = w
            }
            180 -> {
                newWidth = w
                newHeight = h
            }
            0 -> {
                return this
            }
            else -> return this
        }

        val newImage = BufferedImage(newWidth, newHeight, image.type)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = image.getRGB(x, y)
                when (normalizedAngle) {
                    90 -> newImage.setRGB(h - 1 - y, x, pixel)
                    180 -> newImage.setRGB(w - 1 - x, h - 1 - y, pixel)
                    270 -> newImage.setRGB(y, w - 1 - x, pixel)
                }
            }
        }

        return Image(newImage)
    }

    // applies zoom to the image
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
                        // this was implemented given the pdfs sent on classes
                        val a = (x/factor) - (x / factor).toInt()
                        val b = (y/factor) - (y / factor).toInt()
                        val srcX = (x / factor).toInt().coerceIn(0,metadata.width - 2)
                        val srcY = (y / factor).toInt().coerceIn(0,metadata.height - 2)

                        // given the 4 pixel values, it interpolates the value within them
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

    // calculates the histogram of the image
    private fun calculateHistogram(): Map<Int, IntArray> {
        val histogram = mutableMapOf<Int, IntArray>()
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)

        // we just count the frequency of each color
        for (y in 0 until metadata.height) {
            for (x in 0 until metadata.width) {
                val color = Color(image.getRGB(x, y))
                red[color.red]++
                green[color.green]++
                blue[color.blue]++
            }
        }

        histogram[0] = red
        histogram[1] = green
        histogram[2] = blue

        return histogram
    }

    // given an axis, line and channel. We get the profile of the linne which is just a list of pairs (x,f(x))
    fun getLineProfile(axis: Char, lineNumber: Int, channel: Char): List<Pair<Int, Int>> {
        val profile = mutableListOf<Pair<Int, Int>>()
        val limit = if (axis == 'X') {image.width} else {image.height}

        for (i in 0 until limit){
            val color = if (axis == 'X')
                {Color(image.getRGB(i, lineNumber))} else
                {Color(image.getRGB(lineNumber,i))}

            val value = when (channel) {
                'R' -> color.red
                'G' -> color.green
                'B' -> color.blue
                'L' -> luminosity(color) // Grayscale approximation
                else -> 0
            }
            profile.add(i to value)
        }

        return profile
    }

    // easy, just count how many colors are there
    private fun getUniqueColors(): Int{
        val all: MutableSet<Int> = mutableSetOf()
        readAllPixels {x,y,_ ->
            val pixel = image.getRGB(x, y)
            all.add(pixel)
        }
        return all.size
    }

    // checks if the image is grayscale, readAllPixels must return something
    private fun getIsGrayscale(): Boolean{
        var res = true
        readAllPixels {x,y,color ->
            // if any color is not the same as the others res will be false (false & true : false)
            res = res && (color.red == color.green && color.green == color.blue)
            0
        }
        return res
    }

    // checks if the image is binary
    private fun getIsBinary(): Boolean{
        val bin = listOf(0,255)
        var res = true
        readAllPixels {x,y,color ->
            // if any color is not binary res will be false (false & true : false)
            res = res && ((color.red in bin) && (color.green in bin) && (color.blue in bin))
            0
        }
        return res
    }

    // apply a color modification to each pixel
    private fun applyPerPixel(processor: PixelProcessor<Int>): Image {
        val newImage = BufferedImage(metadata.width, metadata.height, BufferedImage.TYPE_INT_RGB)

        readAllPixels {x,y,color ->
            val newRgbValue = processor(x, y, color)
            newImage.setRGB(x, y, newRgbValue)
        }

        return Image(newImage)
    }

}
