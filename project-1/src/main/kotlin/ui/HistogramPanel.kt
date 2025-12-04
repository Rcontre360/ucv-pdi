package org.pdi.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JPanel

class HistogramPanel(private val histogramData: Map<Int, IntArray>) : JPanel() {

    init {
        preferredSize = Dimension(400, 300)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (histogramData.isEmpty()) {
            return
        }

        val width = width
        val height = height
        val barWidth = width / 256.0

        val redData = histogramData[0] ?: IntArray(256)
        val greenData = histogramData[1] ?: IntArray(256)
        val blueData = histogramData[2] ?: IntArray(256)

        val maxCount = (redData + greenData + blueData).maxOrNull() ?: 0

        // Draw Red Histogram
        g.color = Color.RED
        for (i in 0 until 256) {
            val barHeight = (redData[i].toDouble() / maxCount * height).toInt()
            g.fillRect((i * barWidth).toInt(), height - barHeight, barWidth.toInt().coerceAtLeast(1), barHeight)
        }

        // Draw Green Histogram
        g.color = Color.GREEN
        for (i in 0 until 256) {
            val barHeight = (greenData[i].toDouble() / maxCount * height).toInt()
            g.fillRect((i * barWidth).toInt(), height - barHeight, barWidth.toInt().coerceAtLeast(1), barHeight)
        }

        // Draw Blue Histogram
        g.color = Color.BLUE
        for (i in 0 until 256) {
            val barHeight = (blueData[i].toDouble() / maxCount * height).toInt()
            g.fillRect((i * barWidth).toInt(), height - barHeight, barWidth.toInt().coerceAtLeast(1), barHeight)
        }
    }
}