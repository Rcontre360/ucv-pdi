package org.pdi.ui

import org.pdi.core.AppState
import org.pdi.core.Histogram
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class MainButtonsPanel(
    private val state: AppState,
    private val infoPanel: InfoPanel
) : JPanel() {

    private val colorDisplayPanel = JPanel()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        colorDisplayPanel.background = state.color
        colorDisplayPanel.border = BorderFactory.createLineBorder(Color.BLACK)
        colorDisplayPanel.preferredSize = Dimension(20, 20)

        val brightnessSlider = createBrightnessSlider(state)
        val brightnessPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Brillo (Slider)")
            add(brightnessSlider, BorderLayout.CENTER)
            preferredSize = Dimension(400, 70)
            maximumSize = Dimension(Int.MAX_VALUE, 70)
        }

        val contrastSlider = createContrastSlider(state)
        val contrastPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Contrast (Slider)")
            add(contrastSlider, BorderLayout.CENTER)
            preferredSize = Dimension(400, 70)
            maximumSize = Dimension(Int.MAX_VALUE, 70)
        }

        val selectImageButton = createSelectImageButton(state, this)
        val applyGrayscaleButton = createApplyGrayscaleButton(state, this)
        val applyNegativeButton = createApplyNegativeButton(state, this)
        val selectColorButton = createSelectColorButton(state, this) { newColor ->
            colorDisplayPanel.background = newColor
        }

        val showHistogramButton = JButton("Show Histogram").apply {
            addActionListener {
                val histogramData = state.getHistogram()
                if (histogramData != null) {
                    showHistogramWindow(histogramData)
                } else {
                    JOptionPane.showMessageDialog(this@MainButtonsPanel, "No image loaded.", "No Image Selected", JOptionPane.WARNING_MESSAGE)
                }
            }
        }

        val showTonalCurveButton = JButton("Show Tonal Curve").apply {
            addActionListener {
                if (state.getImage() != null) {
                    showTonalCurveWindow()
                } else {
                    JOptionPane.showMessageDialog(this@MainButtonsPanel, "No image loaded or curve data unavailable.", "No Image Selected", JOptionPane.WARNING_MESSAGE)
                }
            }
        }

        val topButtonsPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(selectImageButton)
            add(applyNegativeButton)
            add(applyGrayscaleButton)
            add(selectColorButton)
            add(colorDisplayPanel)
            add(showHistogramButton)
            add(showTonalCurveButton)
            add(ZoomPanel(state))

            val rotate90Button = JButton("Rotate 90°").apply {
                addActionListener {
                    state.rotate(90)
                }
            }
            add(rotate90Button)

            val rotate180Button = JButton("Rotate 180°").apply {
                addActionListener {
                    state.rotate(180)
                }
            }
            add(rotate180Button)

            val rotate270Button = JButton("Rotate 270°").apply {
                addActionListener {
                    state.rotate(270)
                }
            }
            add(rotate270Button)

            val showUmbralizationButton = JButton("Umbralization").apply {
                addActionListener {
                    if (state.isCurrentImageGrayscale()) {
                        showUmbralizationWindow()
                    }
                    else {
                        JOptionPane.showMessageDialog(this@MainButtonsPanel, "Please apply grayscale filter first.", "Grayscale Required", JOptionPane.WARNING_MESSAGE)
                    }
                }
            }
            add(showUmbralizationButton)
        }

        add(topButtonsPanel)
        add(brightnessPanel)
        add(contrastPanel)
    }

    private fun showHistogramWindow(histogramData: Histogram) {
        val histogramFrame = JFrame("Histogram").apply {
            setSize(400, 300)
            add(HistogramPanel(histogramData))
            isVisible = true
        }
        setupWindowDrag(histogramFrame)
    }

    private fun showTonalCurveWindow() {
        val curveData = state.getTonalCurve()
        if (curveData == null) {
            JOptionPane.showMessageDialog(this, "Curve data is not available.", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }

        val curveFrame = JFrame("Tonal Curve Viewer").apply {
            setSize(450, 500)
            layout = BorderLayout()
        }

        val curvePanel = TonalCurvePanel(fullCurveData = curveData)

        val controlPanel = JPanel().apply {
            layout = FlowLayout(FlowLayout.CENTER)
            border = BorderFactory.createTitledBorder("Seleccionar Canal")
        }

        val group = ButtonGroup()

        fun updatePanel(channel: CurveChannel) {
            curvePanel.updateCurve(curveData, channel)
        }

        CurveChannel.entries.forEach { channel ->
            val button = JRadioButton(channel.label).apply {
                addActionListener { updatePanel(channel) }
            }
            group.add(button)
            controlPanel.add(button)

            if (channel == CurveChannel.LUMINOSITY) {
                button.isSelected = true
            }
        }

        curveFrame.add(controlPanel, BorderLayout.NORTH)
        curveFrame.add(curvePanel, BorderLayout.CENTER)

        updatePanel(CurveChannel.LUMINOSITY)

        curveFrame.isVisible = true
        setupWindowDrag(curveFrame)
    }

    private fun showUmbralizationWindow() {
        val umbralizationFrame = JFrame("Umbralization").apply {
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            setSize(400, 300)
            setLocationRelativeTo(SwingUtilities.getWindowAncestor(this@MainButtonsPanel))
        }

        val umbralizationPanel = UmbralizationPanel(state) {
            umbralizationFrame.dispose()
        }

        umbralizationFrame.contentPane.add(umbralizationPanel)
        umbralizationFrame.pack()
        umbralizationFrame.isVisible = true
    }

    private fun setupWindowDrag(frame: JFrame) {
        var initialClick: Point? = null
        frame.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                initialClick = e.point
                frame.contentPane.background = Color.LIGHT_GRAY
            }

            override fun mouseReleased(e: MouseEvent) {
                frame.contentPane.background = UIManager.getColor("Panel.background")
            }
        })

        frame.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val thisX = frame.location.x
                val thisY = frame.location.y
                val xMoved = e.x - initialClick!!.x
                val yMoved = e.y - initialClick!!.y
                frame.setLocation(thisX + xMoved, thisY + yMoved)
            }
        })
    }
}