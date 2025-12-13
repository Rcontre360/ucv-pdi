package org.pdi.ui

import org.pdi.core.AppState
import org.pdi.core.Histogram
import org.pdi.core.StateContext
import org.pdi.io.saveImage
import org.pdi.ui.panels.CurveChannel
import org.pdi.ui.panels.HistogramPanel
import org.pdi.ui.panels.LineProfilePanel
import org.pdi.ui.panels.SaveImagePanel
import org.pdi.ui.panels.TonalCurvePanel
import org.pdi.ui.panels.UmbralizationPanel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

class MainButtonsPanel(
    private val state: AppState,
) : JPanel() {

    init {
        layout = FlowLayout(FlowLayout.LEFT)

        val saveImageButton = JButton("Save Image").apply {
            addActionListener {
                if (state.getImage() != null) {
                    showSaveImageDialog()
                } else {
                    JOptionPane.showMessageDialog(this@MainButtonsPanel, "No image loaded.", "No Image Selected", JOptionPane.WARNING_MESSAGE)
                }
            }
        }

        val selectImageButton = JButton("Select Image").apply {
            addActionListener {
                val fileChooser = JFileChooser()
                val result = fileChooser.showOpenDialog(this@MainButtonsPanel)
                if (result == JFileChooser.APPROVE_OPTION) {
                    val selectedFile: File = fileChooser.selectedFile
                    state.loadImage(selectedFile)
                }
            }
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
                if (state.getTonalCurve() != null) {
                    showTonalCurveWindow()
                } else {
                    JOptionPane.showMessageDialog(this@MainButtonsPanel, "No image loaded or curve data unavailable.", "No Image Selected", JOptionPane.WARNING_MESSAGE)
                }
            }
        }

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

        val showLineProfileButton = JButton("Line Profile").apply {
            addActionListener {
                if (state.getImage() != null) {
                    showLineProfileWindow()
                } else {
                    JOptionPane.showMessageDialog(this@MainButtonsPanel, "No image loaded.", "No Image Selected", JOptionPane.WARNING_MESSAGE)
                }
            }
        }

        add(saveImageButton)
        add(selectImageButton)
        add(showHistogramButton)
        add(showTonalCurveButton)
        add(showUmbralizationButton)
        add(showLineProfileButton)

        state.addContextListener { stateContext: StateContext ->
            saveImageButton.isEnabled = stateContext.currentImage != null
            showHistogramButton.isEnabled = stateContext.currentImage != null
            showTonalCurveButton.isEnabled = stateContext.currentImage != null
            showUmbralizationButton.isEnabled = stateContext.currentImage?.isGrayscale?:false
            showLineProfileButton.isEnabled = stateContext.currentImage != null
        }
    }

    private fun showSaveImageDialog() {
        val saveImageFrame = JFrame("Save Image").apply {
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            setSize(600, 400)
            setLocationRelativeTo(SwingUtilities.getWindowAncestor(this@MainButtonsPanel))
        }

        val saveImagePanel = SaveImagePanel(state) {
            saveImageFrame.dispose()
        }

        saveImageFrame.contentPane.add(saveImagePanel)
        saveImageFrame.pack()
        saveImageFrame.isVisible = true
    }

    private fun showLineProfileWindow() {
        val lineProfileFrame = JFrame("Line Profile").apply {
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            setSize(600, 400)
            setLocationRelativeTo(SwingUtilities.getWindowAncestor(this@MainButtonsPanel))
        }

        val lineProfilePanel = LineProfilePanel(state)

        lineProfileFrame.contentPane.add(lineProfilePanel)
        lineProfileFrame.pack()
        lineProfileFrame.isVisible = true
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

        val curvePanel = TonalCurvePanel(curveLuts = curveData)

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
