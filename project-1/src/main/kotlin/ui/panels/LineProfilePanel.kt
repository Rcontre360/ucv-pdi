package org.pdi.ui.panels

import org.pdi.core.AppState
import java.awt.*
import java.awt.Point
import javax.swing.*
import kotlin.math.roundToInt

class LineProfilePanel(private val appState: AppState) : JPanel() {

    private val xAxisRadioButton = JRadioButton("X Axis")
    private val yAxisRadioButton = JRadioButton("Y Axis")
    private val axisButtonGroup = ButtonGroup()
    private val channelComboBox = JComboBox(arrayOf("R", "G", "B", "Gray"))
    private val graphPanel = LineGraphPanel()
    private val instructionLabel = JLabel("Click the image to select a line.")

    init {
        layout = BorderLayout(10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // Axis selection
        val axisPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            border = BorderFactory.createTitledBorder("Select Axis")
            axisButtonGroup.add(xAxisRadioButton)
            axisButtonGroup.add(yAxisRadioButton)
            add(xAxisRadioButton)
            add(yAxisRadioButton)
            xAxisRadioButton.isSelected = true // Default selection
        }

        // Channel selection
        val channelPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            border = BorderFactory.createTitledBorder("Select Channel")
            add(JLabel("Channel:"))
            add(channelComboBox)
        }

        val controlsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(axisPanel)
            add(channelPanel)
            add(instructionLabel)
        }

        add(controlsPanel, BorderLayout.NORTH)
        add(graphPanel, BorderLayout.CENTER)

        appState.setImageClickListener { point ->
            updateLineProfile(point)
        }

        addAncestorListener(object : javax.swing.event.AncestorListener {
            override fun ancestorAdded(event: javax.swing.event.AncestorEvent?) {}
            override fun ancestorRemoved(event: javax.swing.event.AncestorEvent?) {
                appState.setImageClickListener(null)
            }
            override fun ancestorMoved(event: javax.swing.event.AncestorEvent?) {}
        })
    }

    private fun updateLineProfile(point: Point) {
        val image = appState.context.currentImage
        if (image == null) {
            JOptionPane.showMessageDialog(this, "No image loaded.", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        val axis = if (xAxisRadioButton.isSelected) 'X' else 'Y'
        val lineNumber = if (axis == 'X') point.y else point.x

        val maxLineNumber = if (axis == 'X') image.metadata.height - 1 else image.metadata.width - 1
        if (lineNumber < 0 || lineNumber > maxLineNumber) {
            JOptionPane.showMessageDialog(this, "Line number must be between 0 and $maxLineNumber.", "Invalid Input", JOptionPane.ERROR_MESSAGE)
            return
        }

        val channel = (channelComboBox.selectedItem as String)[0]

        val profileData = appState.context.currentImage?.getLineProfile(axis, lineNumber, channel)

        if (profileData != null) {
            graphPanel.setProfileData(profileData)
        } else {
            JOptionPane.showMessageDialog(this, "Could not generate line profile.", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    class LineGraphPanel : JPanel() {
        private var profileData: List<Pair<Int, Int>> = emptyList()

        fun setProfileData(data: List<Pair<Int, Int>>) {
            this.profileData = data
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            g2d.color = Color.BLACK
            g2d.drawRect(0, 0, width - 1, height - 1)

            if (profileData.isEmpty()) {
                g2d.color = Color.LIGHT_GRAY
                g2d.font = Font("SansSerif", Font.ITALIC, 14)
                g2d.drawString("No profile data available", 50, height / 2)
                return
            }

            val maxIndex = profileData.maxOf { it.first }
            val scaleX = width / maxIndex.toFloat()
            val scaleY = height / 255f // Max pixel value is 255

            g2d.color = Color.BLUE
            g2d.stroke = BasicStroke(1f)

            for (i in 0 until profileData.size - 1) {
                val p1 = profileData[i]
                val p2 = profileData[i + 1]

                val x1 = (p1.first * scaleX).toInt()
                val y1 = (height - p1.second * scaleY).toInt()
                val x2 = (p2.first * scaleX).toInt()
                val y2 = (height - p2.second * scaleY).toInt()

                g2d.drawLine(x1, y1, x2, y2)
            }
        }
    }
}
