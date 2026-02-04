package org.pdi.core.image

import kotlin.math.roundToInt

fun interpolate(lut: IntArray) {
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