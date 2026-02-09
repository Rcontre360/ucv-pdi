package org.pdi.core.quantization

import org.opencv.core.Mat
import org.pdi.core.image.Image
import org.pdi.core.image.ColorBucket // Asumiendo que mueves la data class aquí
import org.pdi.core.image.putRGB
import java.awt.Color
import kotlin.math.hypot

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