package org.pdi.core.image

import kotlin.math.roundToInt

// just to remove some code from the Image class. This applies a stretch to the histogram, increasing contrasst
class Histogram(private val data: Map<Int, IntArray>): Map<Int, IntArray> by data {
    val mins: Map<Int, Int> = data.mapValues { (_, hist) ->
        hist.indices.indexOfFirst { hist[it] > 0 }.coerceAtLeast(0)
    }

    val maxs: Map<Int, Int> = data.mapValues { (_, hist) ->
        hist.indices.indexOfLast { hist[it] > 0 }.coerceAtMost(255)
    }

    val peaks: Map<Int, Int> = data.mapValues { (channel, hist) ->
        val min = mins[channel] ?: 0
        val max = maxs[channel] ?: 255
        // Search only within the active range of the histogram
        (min..max).maxByOrNull { hist[it] } ?: 128
    }

    operator fun get(channel: Int, intensity: Int): Int {
        val channelData = data[channel] ?: return 0
        return if (intensity in channelData.indices) {
            channelData[intensity]
        } else {
            0
        }
    }

    fun stretch(min: Int, max: Int): Map<Int,IntArray> {
        val result = mutableMapOf<Int, IntArray>()

        data.forEach { (channel, _) ->
            val newFreq = IntArray(256) { 0 }
            for (input in 0 until 255) {
                val targetValue = ((input - min).toFloat() / (max - min).toFloat()) * 255.0f
                val outputIntensity = targetValue.roundToInt().coerceIn(0, 255)

                newFreq[input] = outputIntensity
            }

            result[channel] = newFreq
        }

        return result
    }
}