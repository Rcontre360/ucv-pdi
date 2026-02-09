package org.pdi.core.image

import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class Histogram(private val image: Mat) {
    val data: Map<Int, IntArray> = calculate(image)

    val mins: Map<Int, Int> = data.mapValues { (_, hist) ->
        hist.indices.indexOfFirst { hist[it] > 0 }.coerceAtLeast(0)
    }

    val maxs: Map<Int, Int> = data.mapValues { (_, hist) ->
        hist.indices.indexOfLast { hist[it] > 0 }.coerceAtMost(255)
    }

    val peaks: Map<Int, Int> = data.mapValues { (channel, hist) ->
        val min = mins[channel] ?: 0
        val max = maxs[channel] ?: 255
        (min..max).maxByOrNull { hist[it] } ?: 128
    }

    operator fun get(channel: Int, intensity: Int): Int {
        val channelData = data[channel] ?: return 0
        return if (intensity in 0..255) {
            channelData[intensity]
        } else {
            0
        }
    }

    fun stretch(min: Int, max: Int): Mat {
        val yuvChannels = bgrToYuvChannels(image)
        val yChannel = yuvChannels[0]

        val range = (max - min).toDouble().coerceAtLeast(1.0)
        val alpha = 255.0 / range
        val beta = -min * alpha

        val stretchedY = Mat()
        yChannel.convertTo(stretchedY, yChannel.type(), alpha, beta)

        val resultYuv = Mat()
        val combined = listOf(stretchedY, yuvChannels[1], yuvChannels[2])
        Core.merge(combined, resultYuv)

        val resultBgr = Mat()
        Imgproc.cvtColor(resultYuv, resultBgr, Imgproc.COLOR_YUV2BGR)

        yuvChannels.forEach { it.release() }
        stretchedY.release()
        resultYuv.release()

        return resultBgr
    }

    private fun calculate(mat: Mat): Map<Int, IntArray> {
        val result = mutableMapOf<Int, IntArray>()
        val channels = listOf(0, 1, 2)

        channels.forEach { channelIdx ->
            val hist = Mat()
            Imgproc.calcHist(
                listOf(mat),
                MatOfInt(channelIdx),
                Mat(),
                hist,
                MatOfInt(256),
                MatOfFloat(0f, 256f)
            )

            val floatArr = FloatArray(256)
            hist.get(0, 0, floatArr)
            result[channelIdx] = IntArray(256) { floatArr[it].toInt() }
            hist.release()
        }
        return result
    }
}