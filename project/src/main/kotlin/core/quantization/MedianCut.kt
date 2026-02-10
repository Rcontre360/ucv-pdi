package org.pdi.core.quantization

import org.opencv.core.Mat
import org.pdi.core.image.Image
import org.pdi.core.image.euclidean
import org.pdi.core.image.putRGB
import java.awt.Color

data class ColorBucket(val pixels: MutableList<Color>) {
    data class MinMax(var min: Int, var max: Int){
        fun update(n:Int){
            min = minOf(n,min)
            max = maxOf(n, max)
        }
        fun range() = max - min
    }

    var blue = MinMax(256,-1)
    var red = MinMax(256,-1)
    var green = MinMax(256,-1)

    init {
        updateBounds()
    }

    fun updateBounds() {
        blue = MinMax(256,-1)
        red = MinMax(256,-1)
        green = MinMax(256,-1)
        pixels.forEach { pixel ->
            blue.update(pixel.blue)
            red.update(pixel.blue)
            green.update(pixel.blue)
        }
    }

    fun getLongestDimension(): Int {
        val rangeB = blue.range()
        val rangeG = green.range()
        val rangeR = red.range()

        return when {
            rangeR >= rangeG && rangeR >= rangeB -> 0
            rangeG >= rangeR && rangeG >= rangeB -> 1
            else -> 2
        }
    }

    fun getAverageColor(): Color {
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
            val longestBucket = buckets.maxBy { b ->
                maxOf(b.red.range(), b.green.range(), b.blue.range())
            }

            if (longestBucket.pixels.size < 2) break
            buckets.remove(longestBucket)

            val dimension = longestBucket.getLongestDimension()
            longestBucket.pixels.sortBy {
                when(dimension) { 0 -> it.red; 1 -> it.green; else -> it.blue }
            }

            val median = longestBucket.pixels.size / 2

            buckets.remove(longestBucket)
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
            pixel.euclidean(color)
        } ?: Color.BLACK
    }
}