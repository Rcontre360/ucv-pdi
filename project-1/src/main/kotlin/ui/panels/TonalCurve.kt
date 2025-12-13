package org.pdi.ui.panels

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
    private var curveLuts: Map<Char, IntArray> = emptyMap(),
    private var selectedChannel: CurveChannel = CurveChannel.LUMINOSITY
) : JPanel() {
    init {
        preferredSize = Dimension(400, 400)
        background = Color.WHITE
    }

    fun updateCurve(data: Map<Char, IntArray>, channel: CurveChannel) {
        this.curveLuts = data
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

        val channelKey = when(selectedChannel) {
            CurveChannel.RED -> 'R'
            CurveChannel.GREEN -> 'G'
            CurveChannel.BLUE -> 'B'
            CurveChannel.LUMINOSITY -> 'L'
        }
        val lut = curveLuts[channelKey]

        if (lut == null || lut.isEmpty()) {
            g2d.color = Color.LIGHT_GRAY
            g2d.font = Font("SansSerif", Font.ITALIC, 14)
            g2d.drawString("Seleccione una curva o cargue una imagen", 50, height / 2)
            return
        }

        val scaleX = (width -1) / 255.0
        val scaleY = (height -1) / 255.0

        g2d.color = selectedChannel.color
        g2d.stroke = BasicStroke(2f)

        val path = GeneralPath()
        path.moveTo(0.0, height - lut[0] * scaleY)

        for (i in 1..255) {
            val x = i * scaleX
            val y = height - lut[i] * scaleY
            path.lineTo(x, y)
        }

        g2d.draw(path)
    }
}
