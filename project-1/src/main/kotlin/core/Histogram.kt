package org.pdi.core

import kotlin.math.roundToInt

class Histogram(private val data: Map<Int, IntArray>): Map<Int, IntArray> by data {
    fun stretch(min: Int, max: Int): Map<Int,IntArray> {
        val result = mutableMapOf<Int, IntArray>()

        data.forEach { (channel, original) ->
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