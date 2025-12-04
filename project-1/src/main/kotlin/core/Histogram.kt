package org.pdi.core

import kotlin.math.roundToInt

class Histogram(private val data: Map<Int, IntArray>): Map<Int, IntArray> by data {
    fun stretch(min: Float, max: Float): Histogram {
        val result = mutableMapOf<Int, IntArray>()

        data.forEach { (channel, original) ->
            val newFreq = IntArray(255) { 0 }
            for (input in 0 until 255) {
                val targetValue = ((input - min) / (max - min)) * 255.0f
                val outputIntensity = targetValue.roundToInt().coerceIn(0, 255)

                newFreq[outputIntensity] += original[input]
            }

            result[channel] = newFreq
        }

        return Histogram(result)
    }
}