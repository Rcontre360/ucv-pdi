package org.pdi.core

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.pdi.io.toBufferedImage
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.Int
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import org.opencv.core.Core // Import Core for split/merge
import org.opencv.core.TermCriteria

// zoom algorithm type
enum class ZoomAlgorithm {
    CLOSEST_NEIGHTBOUR,
    LINEAR_INTERPOLATION,
}

// callback function for some utilities inside image
typealias PixelProcessor<T> = (x: Int, y: Int, originalColor: DoubleArray) -> T

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
    val buff: BufferedImage by lazy { image.toBufferedImage() }

    // runs a function over all pixels, readonly
    fun readAllPixels(processor: PixelProcessor<Any>) {
        for (y in 0 until image.height()) {
            for (x in 0 until image.width()) {
                val pixel = image.get(y, x)
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
                val srcColor = image.get(y, x)
                val dstColor = resImg.image.get(y, x)

                val srcRed = srcColor[2].toInt()
                val srcGreen = srcColor[1].toInt()
                val srcBlue = srcColor[0].toInt()
                val dstRed = dstColor[2].toInt()
                val dstGreen = dstColor[1].toInt()
                val dstBlue = dstColor[0].toInt()


                // R, G, B channels
                sums[0][srcRed] = sums[0][srcRed] + dstRed
                counts[0][srcRed]++
                sums[1][srcGreen] = sums[1][srcGreen] + dstGreen
                counts[1][srcGreen]++
                sums[2][srcBlue] = sums[2][srcBlue] + dstBlue
                counts[2][srcBlue]++

                // Luminosity channel
                val srcLum = (0.2126 * srcRed + 0.7152 * srcGreen + 0.0722 * srcBlue).roundToInt()
                val dstLum = (0.2126 * dstRed + 0.7152 * dstGreen + 0.0722 * dstBlue).roundToInt()
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
                        doubleArrayOf(0.0, 0.0, 0.0)
                    } else {
                        image.get(imageY, imageX)
                    }

                    imgR[i][j] = color[2].toFloat()
                    imgG[i][j] = color[1].toFloat()
                    imgB[i][j] = color[0].toFloat()
                }
            }

            // we convolute with each channel
            val r = kernel.convolute(imgR).roundToInt().coerceIn(0, 255)
            val g = kernel.convolute(imgG).roundToInt().coerceIn(0, 255)
            val b = kernel.convolute(imgB).roundToInt().coerceIn(0, 255)

            doubleArrayOf(b.toDouble(), g.toDouble(), r.toDouble())
        }
    }

    // this applies a given border operator (we can even mix them since this receives 2 kernels)
    // it applies the kernels and calculates the given gradient
    fun applyBorderOperator(kernelX: Kernel, kernelY: Kernel): Image {
        val imageX = applyKernel(kernelX)
        val imageY = applyKernel(kernelY)

        return applyPerPixel { x, y, _ ->
            // get both colors from both images
            val cx = imageX.image.get(y, x)
            val cy = imageY.image.get(y, x)

            // sqrt( x^2 + y^2 ) onn each channel
            val r = sqrt((cx[2] * cx[2] + cy[2] * cy[2])).toInt().coerceIn(0, 255)
            val g = sqrt((cx[1] * cx[1] + cy[1] * cy[1])).toInt().coerceIn(0, 255)
            val b = sqrt((cx[0] * cx[0] + cy[0] * cy[0])).toInt().coerceIn(0, 255)

            doubleArrayOf(b.toDouble(), g.toDouble(), r.toDouble())
        }
    }

    // just a simple grayscale function. With a small trick were we add a tint to make the grayscale
    // if you pick WHITE it will be the same as a normal grayscale
    fun toGrayscale(tint: Color): Image {
        val (tintR, tintG, tintB) = listOf(tint.red / 255.0, tint.green / 255.0, tint.blue / 255.0)

        return applyPerPixel { _, _, color ->
            val gray = luminosity(color)

            doubleArrayOf(
                (gray * tintB).toInt().coerceIn(0, 255).toDouble(),
                (gray * tintG).toInt().coerceIn(0, 255).toDouble(),
                (gray * tintR).toInt().coerceIn(0, 255).toDouble()
            )
        }
    }

    fun negative(): Image {
        return applyPerPixel { _, _, color ->
            doubleArrayOf(
                255.0 - color[0],
                255.0 - color[1],
                255.0 - color[2]
            )
        }
    }

    // simple brightness function. Instead of adding a constant it multiplies by a factor (between 0-2)
    fun changeBrightness(factor: Float): Image {
        return applyPerPixel { _, _, color ->
            doubleArrayOf(
                (color[0] * (1 + factor)).toInt().coerceIn(0, 255).toDouble(),
                (color[1] * (1 + factor)).toInt().coerceIn(0, 255).toDouble(),
                (color[2] * (1 + factor)).toInt().coerceIn(0, 255).toDouble()
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
            // we just map using the new histogram
            val rMap = newHistogram[0]!!
            val gMap = newHistogram[1]!!
            val bMap = newHistogram[2]!!

            val newR = rMap[color[2].toInt()]
            val newG = gMap[color[1].toInt()]
            val newB = bMap[color[0].toInt()]

            doubleArrayOf(newB.toDouble(), newG.toDouble(), newR.toDouble())
        }
    }

    fun makeThreshold(type:Int): Image {
        val src = this.image
        val gray = Mat()
        val binary = Mat()

        if (src.channels() > 1) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            src.copyTo(gray)
        }

        Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)

        val resultBGR = Mat()
        Imgproc.cvtColor(binary, resultBGR, Imgproc.COLOR_GRAY2BGR)

        return Image(resultBGR)
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
                        val pixel = image.get(srcY, srcX)
                        newImg.put(y, x, *pixel)
                    }

                    ZoomAlgorithm.LINEAR_INTERPOLATION -> {
                        // this was implemented given the pdfs sent on classes
                        val a = (x / factor) - (x / factor).toInt()
                        val b = (y / factor) - (y / factor).toInt()
                        val srcX = (x / factor).toInt().coerceIn(0, metadata.width - 2)
                        val srcY = (y / factor).toInt().coerceIn(0, metadata.height - 2)

                        // given the 4 pixel values, it interpolates the value within them
                        fun interpolateChannel(is_v: Double, ds: Double, ir: Double, dr: Double): Double {
                            return ((1 - a) * (1 - b) * is_v + a * (1 - b) * ds + (1 - a) * b * ir + a * b * dr);
                        }

                        val p_is = image.get(srcY, srcX)
                        val p_ds = image.get(srcY, srcX + 1)
                        val p_ir = image.get(srcY + 1, srcX)
                        val p_dr = image.get(srcY + 1, srcX + 1)

                        val res = doubleArrayOf(
                            interpolateChannel(p_is[0], p_ds[0], p_ir[0], p_dr[0]),
                            interpolateChannel(p_is[1], p_ds[1], p_ir[1], p_dr[1]),
                            interpolateChannel(p_is[2], p_ds[2], p_ir[2], p_dr[2]),
                        )
                        newImg.put(y, x, *res)
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
                val color = image.get(y, x)
                red[color[2].toInt()]++
                green[color[1].toInt()]++
                blue[color[0].toInt()]++
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
                image.get(lineNumber, i)
            } else {
                image.get(i, lineNumber)
            }

            val value = when (channel) {
                'R' -> color[2].toInt()
                'G' -> color[1].toInt()
                'B' -> color[0].toInt()
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
        readAllPixels { x, y, color ->
            all.add(color.joinToString())
        }
        return all.size
    }

    // checks if the image is grayscale, readAllPixels must return something
    private fun getIsGrayscale(): Boolean {
        var res = true
        readAllPixels { x, y, color ->
            // if any color is not the same as the others res will be false (false & true : false)
            res = res && (color[0] == color[1] && color[1] == color[2])
            0
        }
        return res
    }

    // checks if the image is binary
    private fun getIsBinary(): Boolean {
        val bin = listOf(0.0, 255.0)
        var res = true
        readAllPixels { x, y, color ->
            // if any color is not binary res will be false (false & true : false)
            res = res && ((color[0] in bin) && (color[1] in bin) && (color[2] in bin))
            0
        }
        return res
    }

    // apply a color modification to each pixel
    private fun applyPerPixel(processor: PixelProcessor<DoubleArray>): Image {
        // Determine the type for the new image.
        // If the original image had 1 channel (Grayscale), assume the processor returns 1 channel.
        // Otherwise (3 or 4 channels), assume the processor returns 3 channels (BGR).
        val newImageType = if (image.channels() == 1) CvType.CV_8UC1 else CvType.CV_8UC3
        val newImage = Mat.zeros(image.size(), newImageType)

        readAllPixels { x, y, color ->
            val newRgbValue = processor(x, y, color)
            newImage.put(y, x, *newRgbValue)
        }

        return Image(newImage)
    }

    private fun luminosity(color: DoubleArray): Int {
        return (0.2126 * color[2] + 0.7152 * color[1] + 0.0722 * color[0]).roundToInt()
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

    fun dftImage(): Mat {
        val complexImage = performDFT(this.image)
        val magnitude = getDFTLogMagnitude(complexImage)
        complexImage.release()
        return magnitude
    }

    fun highPass(threshold: Double, preserveColor: Boolean): Image {
        if (isGrayscale) {
            val dft = performDFT(this.image)
            shiftQuadrants(dft) // Shift before filtering
            val filter = createFilterMask(dft.cols(), dft.rows(), threshold, inverted = true)
            val filterPlanes = listOf(filter, filter.clone())
            val filterComplex = Mat()
            Core.merge(filterPlanes, filterComplex)

            val filteredDft = Mat()
            Core.multiply(dft, filterComplex, filteredDft)

            shiftQuadrants(filteredDft) // Shift back before inverse DFT

            val inverseDft = Mat()
            Core.idft(filteredDft, inverseDft, Core.DFT_REAL_OUTPUT + Core.DFT_SCALE)

            val result = Mat()
            inverseDft.convertTo(result, CvType.CV_8U)

            dft.release()
            filter.release()
            filterPlanes.forEach { it.release() }
            filterComplex.release()
            filteredDft.release()
            inverseDft.release()

            return Image(result)
        } else {
            // For color images, filter the Y channel
            val yuvChannels = bgrToYuvChannels()
            val yChannel = yuvChannels[0]
            val uChannel = yuvChannels[1]
            val vChannel = yuvChannels[2]

            val dft = performDFT(yChannel)
            shiftQuadrants(dft)
            val filter = createFilterMask(dft.cols(), dft.rows(), threshold, inverted = true)
            val filterPlanes = listOf(filter, filter.clone())
            val filterComplex = Mat()
            Core.merge(filterPlanes, filterComplex)

            val filteredDft = Mat()
            Core.multiply(dft, filterComplex, filteredDft)

            shiftQuadrants(filteredDft)

            val inverseDft = Mat()
            Core.idft(filteredDft, inverseDft, Core.DFT_REAL_OUTPUT + Core.DFT_SCALE)

            val filteredPaddedY = Mat()
            inverseDft.convertTo(filteredPaddedY, CvType.CV_8U)

            // Crop the filtered Y channel back to the original image size
            val filteredY = Mat(filteredPaddedY, Rect(0, 0, uChannel.width(), uChannel.height()))

            val resultMat = if (preserveColor) {
                yuvChannelsToBgr(listOf(filteredY, uChannel, vChannel))
            } else {
                val grayBgr = Mat()
                Imgproc.cvtColor(filteredY, grayBgr, Imgproc.COLOR_GRAY2BGR)
                grayBgr
            }

            // Release all intermediate mats
            yuvChannels.forEach { it.release() }
            dft.release()
            filter.release()
            filterPlanes.forEach { it.release() }
            filterComplex.release()
            filteredDft.release()
            inverseDft.release()
            filteredPaddedY.release()

            return Image(resultMat)
        }
    }

    fun lowPass(threshold: Double, preserveColor: Boolean): Image {
        if (isGrayscale) {
            val dft = performDFT(this.image)
            shiftQuadrants(dft) // Shift before filtering
            val filter = createFilterMask(dft.cols(), dft.rows(), threshold)
            val filterPlanes = listOf(filter, filter.clone())
            val filterComplex = Mat()
            Core.merge(filterPlanes, filterComplex)

            val filteredDft = Mat()
            Core.multiply(dft, filterComplex, filteredDft)

            shiftQuadrants(filteredDft) // Shift back before inverse DFT

            val inverseDft = Mat()
            Core.idft(filteredDft, inverseDft, Core.DFT_REAL_OUTPUT + Core.DFT_SCALE)

            val result = Mat()
            inverseDft.convertTo(result, CvType.CV_8U)

            dft.release()
            filter.release()
            filterPlanes.forEach { it.release() }
            filterComplex.release()
            filteredDft.release()
            inverseDft.release()

            return Image(result)
        } else {
            // For color images, filter the Y channel
            val yuvChannels = bgrToYuvChannels()
            val yChannel = yuvChannels[0]
            val uChannel = yuvChannels[1]
            val vChannel = yuvChannels[2]

            val dft = performDFT(yChannel)
            shiftQuadrants(dft)
            val filter = createFilterMask(dft.cols(), dft.rows(), threshold)
            val filterPlanes = listOf(filter, filter.clone())
            val filterComplex = Mat()
            Core.merge(filterPlanes, filterComplex)

            val filteredDft = Mat()
            Core.multiply(dft, filterComplex, filteredDft)

            shiftQuadrants(filteredDft)

            val inverseDft = Mat()
            Core.idft(filteredDft, inverseDft, Core.DFT_REAL_OUTPUT + Core.DFT_SCALE)

            val filteredPaddedY = Mat()
            inverseDft.convertTo(filteredPaddedY, CvType.CV_8U)

            // Crop the filtered Y channel back to the original image size
            val filteredY = Mat(filteredPaddedY, Rect(0, 0, uChannel.width(), uChannel.height()))

            val resultMat = if (preserveColor) {
                yuvChannelsToBgr(listOf(filteredY, uChannel, vChannel))
            } else {
                val grayBgr = Mat()
                Imgproc.cvtColor(filteredY, grayBgr, Imgproc.COLOR_GRAY2BGR)
                grayBgr
            }

            // Release all intermediate mats
            yuvChannels.forEach { it.release() }
            dft.release()
            filter.release()
            filterPlanes.forEach { it.release() }
            filterComplex.release()
            filteredDft.release()
            inverseDft.release()
            filteredPaddedY.release()

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

    // K-Means Quantization constant
    private val KMEANS_MAX_ITERATIONS = 10

    fun kMeansQuantization(k: Int): Image {
        val originalHeight = image.rows()
        val originalWidth = image.cols()

        // 1. Reshape BGR image to N x 1 Mat where N is total pixels, and each row is a 3-element pixel vector (BGR)
        val samples = image.reshape(0, originalHeight * originalWidth)
        samples.convertTo(samples, CvType.CV_32F) // K-means expects float data

        // 2. Define termination criteria
        val criteria = TermCriteria(
            TermCriteria.EPS + TermCriteria.MAX_ITER,
            KMEANS_MAX_ITERATIONS,
            1.0
        )

        // 3. Perform K-means clustering
        val labels = Mat()
        val centers = Mat() // This will store CV_32F centers initially
        Core.kmeans(
            samples,
            k,
            labels,
            criteria,
            3, // Number of attempts
            Core.KMEANS_PP_CENTERS,
            centers
        )

        // Convert centers to CV_8U (BGR colors) for reconstruction
        centers.convertTo(centers, CvType.CV_8U)

        // 5. Reconstruct the quantized image
        val quantizedBgrMat = Mat(originalHeight, originalWidth, CvType.CV_8UC3)
        for (i in 0 until originalHeight * originalWidth) {
            val label = labels.get(i, 0)[0].toInt()

            // Get the center color vector for this label as bytes
            val centerRowMat = centers.row(label) // centerRowMat is now CV_8U
            val centerColorBytes = ByteArray(3) // Use ByteArray for CV_8U data
            centerRowMat.get(0, 0, centerColorBytes) // This should work now

            quantizedBgrMat.put(i / originalWidth, i % originalWidth, centerColorBytes)
        }

        // Release intermediate Mats
        samples.release()
        labels.release()
        centers.release()

        return Image(quantizedBgrMat)
    }

    fun uniformQuantization(bits: Int): Image {
        if (bits < 0 || bits > 8) {
            throw IllegalArgumentException("Bits per channel must be between 0 and 8.")
        }
        if (bits == 8) {
            return this // No quantization needed if using 8 bits
        }
        if (bits == 0) {
            // All pixels become black if 0 bits are used
            return Image(Mat.zeros(image.size(), image.type()))
        }

        // Calculate the step size for quantization
        val levels = (1 shl bits) // 2^bits
        val step = 256.0 / levels // Size of each quantization step

        return applyPerPixel { _, _, color ->
            val b = (color[0] / step).toInt() * step
            val g = (color[1] / step).toInt() * step
            val r = (color[2] / step).toInt() * step
            doubleArrayOf(b, g, r).map { it.coerceIn(0.0, 255.0) }.toDoubleArray()
        }
    }

    // Helper data class for Median Cut
    private data class ColorBucket(val pixels: MutableList<DoubleArray>) {
        var minB = 256.0;
        var maxB = -1.0
        var minG = 256.0;
        var maxG = -1.0
        var minR = 256.0;
        var maxR = -1.0

        init {
            if (pixels.isNotEmpty()) {
                updateBounds()
            }
        }

        fun updateBounds() {
            minB = 256.0; maxB = -1.0
            minG = 256.0; maxG = -1.0
            minR = 256.0; maxR = -1.0
            pixels.forEach { pixel ->
                minB = minOf(minB, pixel[0])
                maxB = maxOf(maxB, pixel[0])
                minG = minOf(minG, pixel[1])
                maxG = maxOf(maxG, pixel[1])
                minR = minOf(minR, pixel[2])
                maxR = maxOf(maxR, pixel[2])
            }
        }

        fun getLongestDimension(): Int {
            val rangeB = maxB - minB
            val rangeG = maxG - minG
            val rangeR = maxR - minR

            return when {
                rangeR >= rangeG && rangeR >= rangeB -> 2 // Red is longest
                rangeG >= rangeR && rangeG >= rangeB -> 1 // Green is longest
                else -> 0 // Blue is longest or equal
            }
        }

        fun getAverageColor(): DoubleArray {
            if (pixels.isEmpty()) return doubleArrayOf(0.0, 0.0, 0.0)
            var sumB = 0.0;
            var sumG = 0.0;
            var sumR = 0.0
            pixels.forEach { pixel ->
                sumB += pixel[0]
                sumG += pixel[1]
                sumR += pixel[2]
            }
            return doubleArrayOf(sumB / pixels.size, sumG / pixels.size, sumR / pixels.size)
        }
    }

    fun medianCutQuantization(k: Int): Image {
        if (k < 2 || k > 256) { // Limit k to a reasonable range
            throw IllegalArgumentException("Number of colors (k) must be between 2 and 256.")
        }

        // 1. Extract all pixel colors
        val allPixels = mutableListOf<DoubleArray>()
        readAllPixels { _, _, color -> allPixels.add(color.clone()) } // Clone to avoid modifying original array

        if (allPixels.isEmpty()) return this // No pixels to quantize

        // 2. Initialize with one bucket containing all pixels
        val buckets = mutableListOf(ColorBucket(allPixels))

        // 3. Recursively split buckets until k colors are achieved
        while (buckets.size < k) {
            // Find the bucket with the largest range
            val longestBucket = buckets.maxByOrNull { bucket ->
                val rangeB = bucket.maxB - bucket.minB
                val rangeG = bucket.maxG - bucket.minG
                val rangeR = bucket.maxR - bucket.minR
                maxOf(rangeB, rangeG, rangeR)
            } ?: break // Should not happen if buckets not empty

            if (longestBucket.pixels.size < 2) {
                // Cannot split further if bucket has 0 or 1 pixel
                break
            }

            buckets.remove(longestBucket)

            // Split the longest bucket
            val dimension = longestBucket.getLongestDimension()
            longestBucket.pixels.sortBy { it[dimension] } // Sort by the longest dimension
            val medianIndex = longestBucket.pixels.size / 2

            val bucket1Pixels = longestBucket.pixels.subList(0, medianIndex)
            val bucket2Pixels = longestBucket.pixels.subList(medianIndex, longestBucket.pixels.size)

            if (bucket1Pixels.isNotEmpty()) {
                buckets.add(ColorBucket(bucket1Pixels))
            }
            if (bucket2Pixels.isNotEmpty()) {
                buckets.add(ColorBucket(bucket2Pixels))
            }
            // If the split didn't increase the number of buckets, break to prevent infinite loop
            if (buckets.size == buckets.size - 1 + (if (bucket1Pixels.isNotEmpty()) 1 else 0) + (if (bucket2Pixels.isNotEmpty()) 1 else 0)) {
                break
            }
        }

        // 4. Generate the quantized palette (average colors of final buckets)
        val palette = buckets.map { it.getAverageColor() }

        // 5. Reconstruct the image using the new palette
        return applyPerPixel { _, _, pixelColor ->
            findClosestColor(pixelColor, palette)
        }
    }

    private fun findClosestColor(pixel: DoubleArray, palette: List<DoubleArray>): DoubleArray {
        var minDistance = Double.MAX_VALUE
        var closestColor: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)

        palette.forEach { color ->
            val distB = pixel[0] - color[0]
            val distG = pixel[1] - color[1]
            val distR = pixel[2] - color[2]
            val distance = distB * distB + distG * distG + distR * distR // Euclidean distance squared
            if (distance < minDistance) {
                minDistance = distance
                closestColor = color
            }
        }
        return closestColor
    }
}