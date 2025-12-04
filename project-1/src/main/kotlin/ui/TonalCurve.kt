package org.pdi.ui

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.GeneralPath
import javax.swing.JPanel
import kotlin.math.roundToInt


enum class CurveChannel(val label: String, val color: Color) {
    LUMINOSITY("Luminosidad", Color.GRAY),
    RED("Rojo", Color.RED),
    GREEN("Verde", Color.GREEN),
    BLUE("Azul", Color.BLUE)
}

class TonalCurvePanel(
    private var fullCurveData: List<Pair<Color, Color>> = emptyList(),
    private var selectedChannel: CurveChannel = CurveChannel.LUMINOSITY
) : JPanel() {
    init {
        preferredSize = Dimension(400, 400)
        background = Color.WHITE
    }

    private fun Color.getChannelValue(channel: CurveChannel): Int = when (channel) {
        CurveChannel.RED -> this.red
        CurveChannel.GREEN -> this.green
        CurveChannel.BLUE -> this.blue
        CurveChannel.LUMINOSITY -> (0.2126 * this.red + 0.7152 * this.green + 0.0722 * this.blue).roundToInt().coerceIn(0, 255)
    }

    fun updateCurve(data: List<Pair<Color, Color>>, channel: CurveChannel) {
        this.fullCurveData = data
        this.selectedChannel = channel
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2d.color = Color.BLACK
        g2d.drawRect(0, 0, width - 1, height - 1)

        g2d.color = Color(200, 200, 200)
        g2d.drawLine(0, height, width, 0)

        if (fullCurveData.isEmpty()) {
            g2d.color = Color.LIGHT_GRAY
            g2d.font = Font("SansSerif", Font.ITALIC, 14)
            g2d.drawString("Seleccione una curva o cargue una imagen", 50, height / 2)
            return
        }

        val scaleX = width / 255.0
        val scaleY = height / 255.0

        val curvePoints = fullCurveData.map { (inputColor, outputColor) ->
            inputColor.getChannelValue(selectedChannel) to outputColor.getChannelValue(selectedChannel)
        }.toMutableList()

        if (curvePoints.none { it.first == 0 }) {
            curvePoints.add(0 to 0)
        }
        if (curvePoints.none { it.first == 255 }) {
            curvePoints.add(255 to 255)
        }

        curvePoints.sortBy { it.first }

        g2d.color = selectedChannel.color
        g2d.stroke = BasicStroke(3f)

        val path = GeneralPath()

        path.moveTo(0f, height.toFloat())

        for ((inputIntensity, outputIntensity) in curvePoints) {
            val x = (inputIntensity * scaleX).toFloat()
            val y = (height - outputIntensity * scaleY).toFloat()
            path.lineTo(x, y)
        }

        g2d.draw(path)

        g2d.color = Color.BLACK
        g2d.stroke = BasicStroke(1f)
        for ((inputIntensity, outputIntensity) in curvePoints) {
            val x = (inputIntensity * scaleX).toInt()
            val y = (height - outputIntensity * scaleY).toInt()

            g2d.fillOval(x - 4, y - 4, 8, 8)
            g2d.color = selectedChannel.color.darker()
            g2d.drawOval(x - 4, y - 4, 8, 8)
            g2d.color = Color.BLACK
        }
    }
}
