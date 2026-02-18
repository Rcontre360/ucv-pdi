package org.pdi.core.image

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import java.awt.Color
import kotlin.Int
import kotlin.math.roundToInt
import org.opencv.core.Core
import org.pdi.core.kernels.GaussianKernel
import org.pdi.core.kernels.Kernel
import org.pdi.core.quantization.Quantizer
import org.pdi.core.rg.RegionGrowing
import org.pdi.core.transforms.DFT
import org.pdi.core.transforms.FrequencyFilter
import org.pdi.core.transforms.Transform
import kotlin.math.PI
import kotlin.math.atan2

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
class Image(val image: Mat):AutoCloseable {
    // since this image is inmutable all the related variables can be inmutable
    // histogram is only calculated once, same with all the other fields
    val histogram: Histogram by lazy { Histogram(image) }
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

    // returns the tonal curve of the image. The src image is "this" and the target (f(x)) is the "resImg"
    // I decided to make this frequencies based implementation because its more robust than just doing a
    // mapping between one color x to color y. Because for many pixels with color a onn the image they might
    // map to different colors on the dest image. So this handles that use case better.
    fun getTonalCurve(resImg: Image): Map<Char, IntArray> {
        val sums = Array(4) { LongArray(256) }
        val counts = Array(4) { IntArray(256) }

        for (y in 0 until metadata.height) {
            for (x in 0 until metadata.width) {
                val src = image.getRGB(x, y)
                val dst = resImg.image.getRGB(x, y)

                fun update(channelIdx: Int, srcVal: Int, dstVal: Int) {
                    sums[channelIdx][srcVal] += dstVal.toLong()
                    counts[channelIdx][srcVal]++
                }

                update(0, src.red, dst.red)
                update(1, src.green, dst.green)
                update(2, src.blue, dst.blue)
                update(3, luminosity(src), luminosity(dst))
            }
        }

        val keys = listOf('R', 'G', 'B', 'L')
        return keys.mapIndexed { c, char ->
            val lut = IntArray(256) { i ->
                if (counts[c][i] > 0) (sums[c][i] / counts[c][i]).toInt() else -1
            }
            interpolate(lut)
            char to lut
        }.toMap()
    }

    // Returns the raw convolution results as a 2D FloatArray matrix to preserve negative values
    fun applyRawConvolution(kernel: Kernel): Array<FloatArray> {
        val width = image.width()
        val height = image.height()

        // We return a 2D matrix of Floats (the raw "strength" of the gradient)
        val rawMatrix = Array(width) { FloatArray(height) }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val neighborhood = Array(kernel.rows) { FloatArray(kernel.cols) }

                for (i in 0 until kernel.rows) {
                    for (j in 0 until kernel.cols) {
                        val ix = x - kernel.cols / 2 + j
                        val iy = y - kernel.rows / 2 + i

                        val color = if (ix < 0 || ix >= width || iy < 0 || iy >= height) {
                            Color.BLACK
                        } else {
                            image.getRGB(ix, iy)
                        }

                        // Convert RGB to a single Float using your luminosity function
                        neighborhood[i][j] = luminosity(color).toFloat()
                    }
                }
                // Store the convolution result (Negative values are preserved!)
                rawMatrix[x][y] = kernel.convolute(neighborhood)
            }
        }
        return rawMatrix
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

    fun removeBlur(kernel: GaussianKernel):Image{
        return Image(kernel.revert(this.image, 0.1f))
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

    fun getAngleImage(kernelX: Kernel, kernelY: Kernel): Image {
        val imageX = applyRawConvolution(kernelX)
        val imageY = applyRawConvolution(kernelY)

        return applyPerPixel { x, y, _ ->
            val cx = imageX[x][y].toDouble()
            val cy = imageY[x][y].toDouble()

            // angle ranges from -pi to pi
            val angle = atan2(cx,cy)
            val color = (((angle + PI) / (2 * PI)) * 255).toInt()

            Color(color,color,color)
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
        return Image(histogram.equalize(factor * 50))
    }

    fun makeThreshold(type: Int): Image {
        val result = Mat()

        if (this.image.channels() > 1) {
            Imgproc.cvtColor(this.image, result, Imgproc.COLOR_BGR2GRAY)
        } else {
            this.image.copyTo(result)
        }

        if (type == 0) {
            Imgproc.threshold(result, result, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
        } else {
            val threshold = calculateTriangleThreshold(histogram)
            Imgproc.threshold(result, result, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)
        }

        Imgproc.cvtColor(result, result, Imgproc.COLOR_GRAY2BGR)

        return Image(result)
    }

    //function assumes the image is grayscaled. So picks channel 1 on histogram
    private fun calculateTriangleThreshold(hist: Histogram): Int {
        val min = hist.mins[0] ?: 0
        val max = hist.maxs[0] ?: 255
        val peak = hist.peaks[0] ?: 255

        val topAtRightHalf = (peak - min) > (max - peak)
        val tail = if (topAtRightHalf) min else max

        val x0 = peak.toDouble()
        val y0 = hist[0,peak].toDouble()
        val x1 = tail.toDouble()
        val y1 = hist[0,tail].toDouble()

        //vector from peak to tail (easier than line equation)
        val dx = x1 - x0
        val dy = y1 - y0

        return (if (topAtRightHalf) min..peak else peak..max).maxByOrNull { i ->
            val px = i - x0
            val py = hist[0, i] - y0
            // distance
            kotlin.math.abs(dx * py - dy * px)
        } ?: peak
    }

    fun rotate(angle: Int): Image {
        val newSize = calculateRotatedSize(image.cols(), image.rows(), angle)
        val rotationMatrix = Imgproc.getRotationMatrix2D(
            Point(image.cols() / 2.0, image.rows() / 2.0),
            angle.toDouble(),
            1.0
        )

        val currentTx = rotationMatrix.get(0, 2)[0]
        val currentTy = rotationMatrix.get(1, 2)[0]

        rotationMatrix.put(0, 2, currentTx + (newSize.width - image.cols()) / 2.0)
        rotationMatrix.put(1, 2, currentTy + (newSize.height - image.rows()) / 2.0)

        val dst = Mat()
        Imgproc.warpAffine(image, dst, rotationMatrix, newSize)

        rotationMatrix.release()
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
                        newImg.putRGB(x, y, pixel)
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

                        newImg.putRGB(x, y, res)
                    }
                }
            }
        }

        return Image(newImg)
    }

    fun regionGrowing(algorithms: List<RegionGrowing>): Image {
        var resultMat = this.image.clone()

        for (algo in algorithms) {
            val nextMat = algo.execute(resultMat)
            if (resultMat !== this.image) {
                resultMat.release()
            }
            resultMat = nextMat
        }

        return Image(resultMat)
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

    fun applyHLSAdjustments(hueFactor: Int, saturationFactor: Float, lightnessFactor: Float): Image {
        val hlsMat = Mat()
        Imgproc.cvtColor(image, hlsMat, Imgproc.COLOR_BGR2HLS)

        val channels = mutableListOf<Mat>()
        Core.split(hlsMat, channels)

        val newHue = rotateHue(channels[0], hueFactor)
        val newLight = applyChannelFactor(channels[1], lightnessFactor)
        val newSaturation = applyChannelFactor(channels[2], saturationFactor)

        Core.merge(listOf(newHue, newLight, newSaturation), hlsMat)

        val rgb = Mat()
        Imgproc.cvtColor(hlsMat, rgb, Imgproc.COLOR_HLS2BGR)
        listOf(hlsMat, newHue, newLight, newSaturation).release()
        channels.release()

        return Image(rgb)
    }

    fun getTemperature(): Float {
        val yuvMat = Mat()
        Imgproc.cvtColor(image, yuvMat, Imgproc.COLOR_BGR2YUV)

        val channels = mutableListOf<Mat>()
        Core.split(yuvMat, channels)

        val avgU = Core.mean(channels[1]).`val`[0]
        val avgV = Core.mean(channels[2]).`val`[0]
        val diffU = 128.0 - avgU
        val diffV = avgV - 128.0

        val temperature = ((diffU + diffV) / 2.0).toFloat()

        yuvMat.release()
        channels.release()

        return temperature
    }

    fun changeTemperature(tempFactor: Float): Image {
        val yuvMat = Mat()
        Imgproc.cvtColor(image, yuvMat, Imgproc.COLOR_BGR2YUV)

        val channels = mutableListOf<Mat>()
        Core.split(yuvMat, channels)

        val avgU = Core.mean(channels[1]).`val`[0]
        val avgV = Core.mean(channels[2]).`val`[0]

        val whiteBalanceU = 128.0 - avgU
        val whiteBalanceV = 128.0 - avgV

        val finalShiftU = whiteBalanceU - tempFactor
        val finalShiftV = whiteBalanceV + tempFactor

        channels[1].convertTo(channels[1], -1, 1.0, finalShiftU)
        channels[2].convertTo(channels[2], -1, 1.0, finalShiftV)

        val resultMat = Mat()
        Core.merge(channels, resultMat)
        Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_YUV2BGR)

        yuvMat.release()
        channels.forEach { it.release() }

        return Image(resultMat)
    }

    fun phaseImage(space: DFT): Image {
        val freq = space.toFrequency(this.image)
        return Image(space.phase(freq))
    }

    fun frequencyImage(space: Transform): Image {
        return Image(space.generateFrequencyMat(this.image))
    }

    fun frequencyFilter(space: Transform, filter: FrequencyFilter): Image {
        val yuvChannels = rgbToYuv(image)
        val filteredPaddedY = space.applyFilter(yuvChannels[0], filter)

        val filteredY = Mat(filteredPaddedY, Rect(0, 0, yuvChannels[0].cols(), yuvChannels[0].rows()))
        val resultMat = yuvToRGB(listOf(filteredY, yuvChannels[1], yuvChannels[2]))

        yuvChannels.release()
        filteredPaddedY.release()
        filteredY.release()

        return Image(resultMat)
    }

    fun applyQuantization(quantizer: Quantizer): Image {
        val quantizedMat = quantizer.apply(this)
        return Image(quantizedMat)
    }

    // runs a function over all pixels, readonly
    fun readAllPixels(processor: PixelProcessor<Unit>) {
        for (y in 0 until image.height()) {
            for (x in 0 until image.width()) {
                val pixel = image.getRGB(x, y)
                processor(x, y, pixel)
            }
        }
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

    override fun close() {
        if (!image.empty()) {
            image.release()
        }
    }
}
