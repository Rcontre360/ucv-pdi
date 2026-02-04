package org.pdi.core.image

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.awt.Color
import kotlin.Int
import kotlin.math.roundToInt
import kotlin.random.Random
import org.opencv.core.Core // Import Core for split/merge
import org.pdi.core.Kernel

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
class Image(val image: Mat) {
    // since this image is inmutable all the related variables can be inmutable
    // histogram is only calculated once, same with all the other fields
    val histogram: Histogram by lazy { Histogram(calculateHistogram()) }
    val isGrayscale = getIsGrayscale()
    val isBinary = getIsBinary()
    val metadata = Metadata(
        width = image.width(),
        height = image.height(),
        uniqueColors = getUniqueColors(),
        bitsPerPixel = if (isBinary) {
            1
        } else {
            if (isGrayscale) {
                8
            } else {
                24
            }
        },
    )

    // runs a function over all pixels, readonly
    fun readAllPixels(processor: PixelProcessor<Unit>) {
        for (y in 0 until image.height()) {
            for (x in 0 until image.width()) {
                val pixel = image.getRGB(x, y)
                processor(x, y, pixel)
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
                val src = image.getRGB(x, y)
                val dst = image.getRGB(x, y)

                // R, G, B channels
                sums[0][src.red] = sums[0][src.red] + dst.red
                counts[0][src.red]++
                sums[1][src.green] = sums[1][src.green] + dst.green
                counts[1][src.green]++
                sums[2][src.blue] = sums[2][src.blue] + dst.blue
                counts[2][src.blue]++

                // Luminosity channel
                val srcLum = luminosity(src)
                val dstLum = luminosity(dst)

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
                val prevY = if (prevX < 0) {
                    lut[nextX]
                } else {
                    lut[prevX]
                }
                val nextY = if (nextX >= lut.size) {
                    lut[prevX]
                } else {
                    lut[nextX]
                }
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
    fun applyKernel(kernel: Kernel): Image {
        // for each pixel we do the following. Calculate the window surrounding that pixel, the size of the kernel
        return applyPerPixel { x, y, _ ->
            // the matrixes that will hold the image windows
            val imgR = Array(kernel.rows) { FloatArray(kernel.cols) }
            val imgG = Array(kernel.rows) { FloatArray(kernel.cols) }
            val imgB = Array(kernel.rows) { FloatArray(kernel.cols) }

            for (i in 0 until kernel.rows) {
                for (j in 0 until kernel.cols) {
                    // we calculate the window index
                    val imageX = (x - kernel.cols / 2 + j)
                    val imageY = (y - kernel.rows / 2 + i)
                    // if the window is OUTSIDE the image. We apply some "padding" with black, this wont mess with borders
                    val color = if (imageX < 0 || imageX >= image.width() || imageY < 0 || imageY >= image.height()) {
                        Color.black
                    } else {
                        image.getRGB(imageX,imageY)
                    }

                    imgR[i][j] = color.red.toFloat()
                    imgG[i][j] = color.green.toFloat()
                    imgB[i][j] = color.blue.toFloat()
                }
            }

            Color(
                kernel.convolute(imgR).roundToInt().coerceIn(0, 255),
                kernel.convolute(imgG).roundToInt().coerceIn(0, 255),
                kernel.convolute(imgB).roundToInt().coerceIn(0, 255)
            )
        }
    }

    // this applies a given border operator (we can even mix them since this receives 2 kernels)
    // it applies the kernels and calculates the given gradient
    fun applyBorderOperator(kernelX: Kernel, kernelY: Kernel): Image {
        val imageX = applyKernel(kernelX)
        val imageY = applyKernel(kernelY)

        return applyPerPixel { x, y, _ ->
            // get both colors from both images
            val cx = imageX.image.getRGB(x,y)
            val cy = imageY.image.getRGB(x,y)

            Color(
                kotlin.math.hypot(cx.red.toDouble(),cy.red.toDouble()).toInt().coerceIn(0, 255),
                kotlin.math.hypot(cx.green.toDouble(),cy.green.toDouble()).toInt().coerceIn(0, 255),
                kotlin.math.hypot(cx.blue.toDouble(),cy.blue.toDouble()).toInt().coerceIn(0, 255)
            )
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
            )
        }
    }

    fun negative(): Image {
        return applyPerPixel { _, _, color ->
            Color(
                255 - color.red,
                255 - color.green,
                255 - color.blue
            )
        }
    }

    // simple brightness function. Instead of adding a constant it multiplies by a factor (between 0-2)
    fun changeBrightness(factor: Float): Image {
        return applyPerPixel { _, _, color ->
            Color(
                (color.red * (1 + factor)).toInt().coerceIn(0, 255),
                (color.green * (1 + factor)).toInt().coerceIn(0, 255),
                (color.blue * (1 + factor)).toInt().coerceIn(0, 255)
            )
        }
    }

    // changes the image contrast. The method used is stretching the histogram
    // to pick the min/max limits we allow the user to do it on will. Since most apps allow the user to change the
    // contrast "level". We use a factor between 0-1. 0.1 would be stretching the histogram from 12.7 to 255-12.7
    fun changeContrast(factor: Float): Image {
        val min = (127 * factor)
        val max = 255 - (127 * factor)
        val newHistogram = histogram.stretch(min.toInt(), max.toInt())

        return applyPerPixel { _, _, color ->
            val rMap = newHistogram[0]!!
            val gMap = newHistogram[1]!!
            val bMap = newHistogram[2]!!

            Color(rMap[color.red], gMap[color.green], bMap[color.blue])
        }
    }

    fun makeThreshold(type: Int): Image {
        val src = this.image
        val gray = Mat()
        val binary = Mat()

        // 1. Ensure we are working with Grayscale
        if (src.channels() > 1) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            src.copyTo(gray)
        }

        if (type == 0) {
            // --- OTSU IMPLEMENTATION ---
            Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
        } else {
            // --- TRIANGLE IMPLEMENTATION ---
            // Assuming channel 0 is the one used for the grayscale histogram
            val histData = histogram[0] ?: IntArray(256) { 0 }
            val threshold = calculateTriangleThreshold(histData)

            Imgproc.threshold(gray, binary, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)
        }

        // Convert back to 3-channel BGR to avoid the IndexOutOfBounds error
        val resultBGR = Mat()
        Imgproc.cvtColor(binary, resultBGR, Imgproc.COLOR_GRAY2BGR)

        return Image(resultBGR)
    }

    private fun calculateTriangleThreshold(hist: IntArray): Int {
        // 1. Find boundaries: First and last non-zero bins
        var min = 0
        while (min < 255 && hist[min] == 0) min++

        var max = 255
        while (max > 0 && hist[max] == 0) max--

        // 2. Find the peak (highest frequency)
        var maxFreq = -1
        var peakIndex = -1
        for (i in min..max) {
            if (hist[i] > maxFreq) {
                maxFreq = hist[i]
                peakIndex = i
            }
        }

        // 3. Determine the tail side.
        // If peak is closer to min, tail is at max. If closer to max, tail is at min.
        val isTailAtMax = (peakIndex - min) > (max - peakIndex)

        // We only iterate between the peak and the chosen tail
        val rangeStart = if (isTailAtMax) min else peakIndex
        val rangeEnd = if (isTailAtMax) peakIndex else max

        var bestThreshold = peakIndex
        var maxDistance = -1.0

        // Coordinates of the two points defining our line
        val x0 = peakIndex.toDouble()
        val y0 = hist[peakIndex].toDouble()
        val x1 = (if (isTailAtMax) min else max).toDouble()
        val y1 = hist[if (isTailAtMax) min else max].toDouble()

        // 4. Geometry calculation
        // Line: ax + by + c = 0
        val a = y1 - y0
        val b = x0 - x1
        val c = x1 * y0 - y1 * x0
        val denominator = kotlin.math.sqrt(a * a + b * b)

        if (denominator == 0.0) return peakIndex // Safety check

        for (i in rangeStart..rangeEnd) {
            // Absolute distance from point (i, hist[i]) to the line
            val dist = kotlin.math.abs(a * i + b * hist[i] + c) / denominator

            if (dist > maxDistance) {
                maxDistance = dist
                bestThreshold = i
            }
        }

        return bestThreshold
    }

    fun rotate(angle: Int): Image {
        val dst = Mat()
        val rotationMatrix = Imgproc.getRotationMatrix2D(
            Point(image.cols() / 2.0, image.rows() / 2.0),
            angle.toDouble(),
            1.0
        )
        Imgproc.warpAffine(image, dst, rotationMatrix, Size(image.cols().toDouble(), image.rows().toDouble()))
        return Image(dst)
    }

    fun translate(dx: Int, dy: Int): Image {
        val dst = Mat()
        val translationMatrix = Mat(2, 3, CvType.CV_32F)
        translationMatrix.put(0, 0, 1.0) // Scale X
        translationMatrix.put(0, 1, 0.0)
        translationMatrix.put(0, 2, dx.toDouble()) // Translate X
        translationMatrix.put(1, 0, 0.0)
        translationMatrix.put(1, 1, 1.0) // Scale Y
        translationMatrix.put(1, 2, dy.toDouble()) // Translate Y

        Imgproc.warpAffine(image, dst, translationMatrix, image.size())
        translationMatrix.release()
        return Image(dst)
    }

    // applies zoom to the image
    fun zoom(factor: Float, algo: ZoomAlgorithm): Image {
        val w = (metadata.width * factor).toInt()
        val h = (metadata.height * factor).toInt()
        val newImg = Mat(h, w, image.type())

        for (y in 0 until h) {
            for (x in 0 until w) {

                when (algo) {
                    ZoomAlgorithm.CLOSEST_NEIGHTBOUR -> {
                        // when factor < 0 it acts as a multiplicationn. Sometimes it gets out of range
                        val srcX = (x / factor).toInt().coerceIn(0, metadata.width - 1)
                        val srcY = (y / factor).toInt().coerceIn(0, metadata.height - 1)
                        val pixel = image.getRGB(srcX,srcY)
                        newImg.putRGB(y, x, pixel)
                    }

                    ZoomAlgorithm.LINEAR_INTERPOLATION -> {
                        // this was implemented given the pdfs sent on classes
                        val a = (x / factor) - (x / factor).toInt()
                        val b = (y / factor) - (y / factor).toInt()
                        val srcX = (x / factor).toInt().coerceIn(0, metadata.width - 2)
                        val srcY = (y / factor).toInt().coerceIn(0, metadata.height - 2)

                        // given the 4 pixel values, it interpolates the value within them
                        fun interpolateChannel(isV: Int, ds: Int, ir: Int, dr: Int): Int {
                            return ((1 - a) * (1 - b) * isV + a * (1 - b) * ds + (1 - a) * b * ir + a * b * dr).toInt()
                        }

                        val pIs = image.getRGB(srcX,srcY)
                        val pDs = image.getRGB( srcX + 1,srcY)
                        val pIr = image.getRGB( srcX,srcY + 1)
                        val pDr = image.getRGB( srcX + 1,srcY + 1)

                        val res = Color(
                            interpolateChannel(pIs.red, pDs.red, pIr.red, pDr.red),
                            interpolateChannel(pIs.green, pDs.green, pIr.green, pDr.green),
                            interpolateChannel(pIs.blue, pDs.blue, pIr.blue, pDr.blue),
                        )

                        newImg.putRGB(y, x, res)
                    }
                }
            }
        }

        return Image(newImg)
    }

    fun regionGrowing(seeds: List<Point>, maxDiff: Int, connectivity: Int): Image {
        // Create a single-channel grayscale version of the input image for floodFill
        val singleChannelImage = Mat()
        // 'image' is the 3-channel grayscale input where R=G=B, so COLOR_BGR2GRAY correctly extracts one channel
        Imgproc.cvtColor(image, singleChannelImage, Imgproc.COLOR_BGR2GRAY)

        // Mask must be 2 pixels larger than the image and 8-bit single channel.
        val mask = Mat.zeros(metadata.height + 2, metadata.width + 2, CvType.CV_8UC1)
        var regionLabel = 1

        val diffScalar = Scalar(maxDiff.toDouble())

        for (seed in seeds) {
            // Check if the seed point has already been filled
            // Note: mask coordinates are (y+1, x+1)
            if (mask.get(seed.y.toInt() + 1, seed.x.toInt() + 1)[0] == 0.0) {
                // The new value to fill the mask with is encoded in the flags
                val flags = connectivity or Imgproc.FLOODFILL_MASK_ONLY or (regionLabel shl 8)
                Imgproc.floodFill(singleChannelImage, mask, seed, Scalar(0.0), Rect(), diffScalar, diffScalar, flags)
                regionLabel++
            }
        }
        singleChannelImage.release() // Release the temporary single-channel image

        // Create a list of random colors for tinting, one for each region label
        val random = Random(System.currentTimeMillis())
        val regionTints = (0 until regionLabel).map {
            Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }

        val resultMat = Mat.zeros(image.size(), CvType.CV_8UC3)
        for (y in 0 until metadata.height) {
            for (x in 0 until metadata.width) {
                // Adjust coordinates for the larger mask
                val label = mask.get(y + 1, x + 1)[0].toInt()
                val originalPixel = image.get(y, x)[0] // Grayscale value from the 3-channel input

                if (label > 0) {
                    val tint = regionTints[label]
                    val (tintR, tintG, tintB) = listOf(tint.red / 255.0, tint.green / 255.0, tint.blue / 255.0)
                    resultMat.put(
                        y, x,
                        (originalPixel * tintB).coerceIn(0.0, 255.0),
                        (originalPixel * tintG).coerceIn(0.0, 255.0),
                        (originalPixel * tintR).coerceIn(0.0, 255.0)
                    )
                } else {
                    resultMat.put(y, x, originalPixel, originalPixel, originalPixel)
                }
            }
        }

        mask.release()
        return Image(resultMat)
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
                val color = image.getRGB(x, y)
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
        val limit = if (axis == 'X') {
            image.width()
        } else {
            image.height()
        }

        for (i in 0 until limit) {
            val color = if (axis == 'X') {
                image.getRGB(i,lineNumber)
            } else {
                image.getRGB(lineNumber,i)
            }

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
    private fun getUniqueColors(): Int {
        val all: MutableSet<String> = mutableSetOf()
        readAllPixels {_, _, color ->
            all.add(color.toString())
        }
        return all.size
    }

    // checks if the image is grayscale, readAllPixels must return something
    private fun getIsGrayscale(): Boolean {
        var res = true
        readAllPixels { _, _, color ->
            // if any color is not the same as the others res will be false (false & true : false)
            res = res && (color.red == color.blue && color.blue == color.green)
        }
        return res
    }

    // checks if the image is binary
    private fun getIsBinary(): Boolean {
        val bin = listOf(0, 255)
        var res = true
        readAllPixels { _, _, color ->
            // if any color is not binary res will be false (false & true : false)
            res = res && ((color.red in bin) && (color.green in bin) && (color.blue in bin));
        }
        return res
    }

    // apply a color modification to each pixel
    fun applyPerPixel(processor: PixelProcessor<Color>): Image {
        // Determine the type for the new image.
        // If the original image had 1 channel (Grayscale), assume the processor returns 1 channel.
        // Otherwise (3 or 4 channels), assume the processor returns 3 channels (BGR).
        val newImageType = if (image.channels() == 1) CvType.CV_8UC1 else CvType.CV_8UC3
        val newImage = Mat.zeros(image.size(), newImageType)

        readAllPixels { x, y, color ->
            val newRgbValue = processor(x, y, color)
            newImage.putRGB(x, y, newRgbValue)
        }

        return Image(newImage)
    }

    fun applyHLSAdjustments(hueFactor: Int, saturationFactor: Float, lightnessFactor: Float): Image {
        val hlsMat = Mat()
        Imgproc.cvtColor(image, hlsMat, Imgproc.COLOR_BGR2HLS)

        val channels = arrayListOf<Mat>()
        Core.split(hlsMat, channels)

        val hueChannel = channels[0]
        val lightnessChannel = channels[1]
        val saturationChannel = channels[2]

        for (y in 0 until hlsMat.rows()) {
            for (x in 0 until hlsMat.cols()) {
                // Hue adjustment
                val currentHue = hueChannel.get(y, x)[0]
                var newHue = currentHue + hueFactor

                while (newHue < 0) newHue += 180.0
                newHue %= 180.0
                hueChannel.put(y, x, newHue)

                // Lightness adjustment
                val currentLightness = lightnessChannel.get(y, x)[0]
                val newLightness = (currentLightness + (lightnessFactor * 255)).coerceIn(0.0, 255.0)
                lightnessChannel.put(y, x, newLightness)

                // Saturation adjustment
                val currentSaturation = saturationChannel.get(y, x)[0]
                val newSaturation = (currentSaturation + (saturationFactor * 255)).coerceIn(0.0, 255.0)
                saturationChannel.put(y, x, newSaturation)
            }
        }

        Core.merge(channels, hlsMat)
        val resultMat = Mat()
        Imgproc.cvtColor(hlsMat, resultMat, Imgproc.COLOR_HLS2BGR)

        hlsMat.release()
        channels.forEach { it.release() }

        return Image(resultMat)
    }

    fun applyYUVAdjustments(yFactor: Float, uFactor: Float, vFactor: Float): Image {
        val yuvMat = Mat()
        Imgproc.cvtColor(image, yuvMat, Imgproc.COLOR_BGR2YUV)

        val channels = arrayListOf<Mat>()
        Core.split(yuvMat, channels)

        // Convert channels to 32-bit float for precise arithmetic
        val yChannelFloat = Mat()
        val uChannelFloat = Mat()
        val vChannelFloat = Mat()

        channels[0].convertTo(yChannelFloat, CvType.CV_32F)
        channels[1].convertTo(uChannelFloat, CvType.CV_32F)
        channels[2].convertTo(vChannelFloat, CvType.CV_32F)

        // Apply adjustments using Core.add
        Core.add(yChannelFloat, Scalar(yFactor * 255.0), yChannelFloat)
        Core.add(uChannelFloat, Scalar(uFactor * 255.0), uChannelFloat)
        Core.add(vChannelFloat, Scalar(vFactor * 255.0), vChannelFloat)

        // Clamp values to 0-255 and convert back to 8-bit unsigned
        val adjustedY = Mat()
        val adjustedU = Mat()
        val adjustedV = Mat()

        // convertTo to CV_8U automatically clamps values to 0-255 range.
        yChannelFloat.convertTo(adjustedY, CvType.CV_8U)
        uChannelFloat.convertTo(adjustedU, CvType.CV_8U)
        vChannelFloat.convertTo(adjustedV, CvType.CV_8U)

        // Merge adjusted channels back into a single YUV Mat
        val adjustedYUVMat = Mat()
        val adjustedChannelsList = listOf(adjustedY, adjustedU, adjustedV)
        Core.merge(adjustedChannelsList, adjustedYUVMat)

        val resultMat = Mat()
        Imgproc.cvtColor(adjustedYUVMat, resultMat, Imgproc.COLOR_YUV2BGR)

        // Release all intermediate Mats
        yuvMat.release()
        channels.forEach { it.release() }
        yChannelFloat.release()
        uChannelFloat.release()
        vChannelFloat.release()
        adjustedY.release()
        adjustedU.release()
        adjustedV.release()
        adjustedYUVMat.release()

        return Image(resultMat)
    }

    fun dftImage(): Image {
        val complexImage = performDFT(this.image)
        val magnitude = getDFTLogMagnitude(complexImage)
        complexImage.release()

        return Image(magnitude)
    }

    fun highPass(threshold: Double, preserveColor: Boolean): Image =
        frequencyFilterWrapper(threshold, preserveColor, inverted = true)

    fun lowPass(threshold: Double, preserveColor: Boolean): Image =
        frequencyFilterWrapper(threshold, preserveColor, inverted = false)

    private fun frequencyFilterWrapper(threshold: Double, preserveColor: Boolean, inverted: Boolean): Image {
        if (isGrayscale) {
            val result = applyFrequencyFilter(this.image, threshold, inverted)
            return Image(result)
        } else {
            val yuvChannels = bgrToYuvChannels()
            val filteredPaddedY = applyFrequencyFilter(yuvChannels[0], threshold, inverted)

            // Crop back to original size
            val filteredY = Mat(filteredPaddedY, Rect(0, 0, yuvChannels[1].width(), yuvChannels[1].height()))

            val resultMat = if (preserveColor) {
                yuvChannelsToBgr(listOf(filteredY, yuvChannels[1], yuvChannels[2]))
            } else {
                val grayBgr = Mat()
                Imgproc.cvtColor(filteredY, grayBgr, Imgproc.COLOR_GRAY2BGR)
                grayBgr
            }

            yuvChannels.forEach { it.release() }
            filteredPaddedY.release()
            // filteredY is a header, it doesn't strictly need release() but good practice
            filteredY.release()

            return Image(resultMat)
        }
    }

    // Private helper functions for color space conversions
    private fun bgrToYuvChannels(): List<Mat> {
        val yuvMat = Mat()
        Imgproc.cvtColor(image, yuvMat, Imgproc.COLOR_BGR2YUV)
        val channels = arrayListOf<Mat>()
        Core.split(yuvMat, channels)
        yuvMat.release()
        return channels
    }

    private fun yuvChannelsToBgr(channels: List<Mat>): Mat {
        val yuvMat = Mat()
        Core.merge(channels, yuvMat)
        val bgrMat = Mat()
        Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2BGR)
        yuvMat.release()
        return bgrMat
    }
    
    fun kMeansQuantization(k: Int): Image {
        return Image(org.pdi.core.image.kMeansQuantization(image, k))
    }

    fun uniformQuantization(bits: Int): Image {
        return Image(org.pdi.core.image.uniformQuantization(this, bits))
    }

    fun medianCutQuantization(k: Int): Image {
        return org.pdi.core.image.medianCutQuantization(this, k)
    }
}
