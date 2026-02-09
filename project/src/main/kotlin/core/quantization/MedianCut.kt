package org.pdi.core.quantization

import org.opencv.core.Mat
import org.pdi.core.image.Image
import org.pdi.core.image.putRGB
import java.awt.Color
import kotlin.math.hypot

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

class MedianCutQuantizer(k: Int) : Quantizer(k) {
    override fun apply(image: Image): Mat {
        val allPixels = mutableListOf<Color>()
        image.readAllPixels { _, _, color -> allPixels.add(color) }

        val buckets = mutableListOf(ColorBucket(allPixels))

        while (buckets.size < value) {
            val longestBucket = buckets.maxByOrNull { b ->
                maxOf(b.maxR - b.minR, b.maxG - b.minG, b.maxB - b.minB)
            } ?: break

            if (longestBucket.pixels.size < 2) break
            buckets.remove(longestBucket)

            val dimension = longestBucket.getLongestDimension()
            longestBucket.pixels.sortBy {
                when(dimension) { 0 -> it.red; 1 -> it.green; else -> it.blue }
            }

            val median = longestBucket.pixels.size / 2
            buckets.add(ColorBucket(longestBucket.pixels.subList(0, median)))
            buckets.add(ColorBucket(longestBucket.pixels.subList(median, longestBucket.pixels.size)))
        }

        val palette = buckets.map { it.getAverageColor() }
        val result = Mat.zeros(image.image.size(), image.image.type())

        image.readAllPixels { x, y, c ->
            val closest = findClosestColor(c, palette)
            result.putRGB(x, y, closest)
        }
        return result
    }

    private fun findClosestColor(pixel: Color, palette: List<Color>): Color {
        return palette.minByOrNull { color ->
            hypot((pixel.red - color.red).toDouble(),
                hypot((pixel.green - color.green).toDouble(),
                    (pixel.blue - color.blue).toDouble()))
        } ?: Color.BLACK
    }
}