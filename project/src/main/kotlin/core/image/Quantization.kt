package org.pdi.core.image

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.TermCriteria
import java.awt.Color
import kotlin.math.hypot

// K-Means Quantization constant
private val KMEANS_MAX_ITERATIONS = 10

// Helper data class for Median Cut
data class ColorBucket(val pixels: MutableList<Color>) {
    var minB = 256
    var maxB = -1
    var minG = 256
    var maxG = -1
    var minR = 256
    var maxR = -1

    init {
        if (pixels.isNotEmpty()) {
            updateBounds()
        }
    }

    fun updateBounds() {
        minB = 256; maxB = -1
        minG = 256; maxG = -1
        minR = 256; maxR = -1
        pixels.forEach { pixel ->
            minR = minOf(minR, pixel.red)
            maxR = maxOf(maxR, pixel.red)
            minG = minOf(minG, pixel.green)
            maxG = maxOf(maxG, pixel.green)
            minB = minOf(minB, pixel.blue)
            maxB = maxOf(maxB, pixel.blue)
        }
    }

    fun getLongestDimension(): Int {
        val rangeB = maxB - minB
        val rangeG = maxG - minG
        val rangeR = maxR - minR

        return when {
            rangeR >= rangeG && rangeR >= rangeB -> 0 // Red is longest
            rangeG >= rangeR && rangeG >= rangeB -> 1 // Green is longest
            else -> 2 // Blue is longest or equal
        }
    }

    fun getAverageColor(): Color {
        if (pixels.isEmpty()) return Color.black

        val sum = pixels.fold(intArrayOf(0,0,0)) { acc, pixel ->
            intArrayOf(
                acc[0] + pixel.red,
                acc[1] + pixel.green,
                acc[2] + pixel.blue
            )
        }

        return Color(
            (sum[0] / pixels.size).coerceIn(0,255),
            (sum[1] / pixels.size).coerceIn(0,255),
            (sum[2] / pixels.size).coerceIn(0,255)
        )
    }
}

fun kMeansQuantization(image: Mat, k: Int): Mat {
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

        val b = centerColorBytes[0].toInt() and 0xFF
        val g = centerColorBytes[1].toInt() and 0xFF
        val r = centerColorBytes[2].toInt() and 0xFF
        quantizedBgrMat.put(i / originalWidth, i % originalWidth, b.toDouble(), g.toDouble(), r.toDouble())
    }

    // Release intermediate Mats
    samples.release()
    labels.release()
    centers.release()

    return quantizedBgrMat
}

fun uniformQuantization(image: Image, bits: Int): Mat {
    if (bits < 0 || bits > 8) {
        throw IllegalArgumentException("Bits per channel must be between 0 and 8.")
    }
    if (bits == 8) {
        return image.image // No quantization needed if using 8 bits
    }
    if (bits == 0) {
        // All pixels become black if 0 bits are used
        return Mat.zeros(image.image.size(), image.image.type())
    }

    // Calculate the step size for quantization
    val levels = (1 shl bits) // 2^bits
    val step = 256.0 / levels // Size of each quantization step

    val newImage = Mat.zeros(image.image.size(), image.image.type())

    image.readAllPixels { x, y, color ->
        val b = ((color.blue / step).toInt() * step).toInt().coerceIn(0,255)
        val g = ((color.green / step).toInt() * step).toInt().coerceIn(0,255)
        val r = ((color.red / step).toInt() * step).toInt().coerceIn(0,255)

        newImage.putRGB(x, y, Color(b, g, r))
    }
    return newImage
}

fun medianCutQuantization(image: Image, k: Int): Image {
    if (k < 2 || k > 256) { // Limit k to a reasonable range
        throw IllegalArgumentException("Number of colors (k) must be between 2 and 256.")
    }

    // 1. Extract all pixel colors
    val allPixels = mutableListOf<Color>()
    image.readAllPixels { _, _, color -> allPixels.add(color) } // Clone to avoid modifying original array

    if (allPixels.isEmpty()) return image // No pixels to quantize

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
        val comparator = when (dimension) {
            0 -> compareBy<Color> { it.red }
            1 -> compareBy<Color> { it.green }
            else -> compareBy<Color> { it.blue }
        }
        longestBucket.pixels.sortWith(comparator)

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
    return image.applyPerPixel { _, _, c ->
        findClosestColor(c, palette)
    }
}

private fun findClosestColor(pixel: Color, palette: List<Color>): Color {
    var minDistance = Double.MAX_VALUE
    var closest = Color.black

    palette.forEach { color ->
        val distR = pixel.red - color.red
        val distG = pixel.green - color.green
        val distB = pixel.blue - color.blue
        val distance = hypot(distR.toDouble(), hypot(distG.toDouble(), distB.toDouble()))

        if (distance < minDistance) {
            minDistance = distance
            closest = color
        }
    }
    return closest
}