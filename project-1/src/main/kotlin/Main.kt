package org.pdi

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import org.pdi.core.*
import org.pdi.ui.*

private val imageLabel = JLabel()
private val colorDisplayPanel = JPanel()
private lateinit var mainFrame: JFrame

fun main(args: Array<String>) {
    val state = AppState()
    SwingUtilities.invokeLater {
        createAndShowGUI(state)
    }
}

// Función auxiliar para actualizar la vista de la imagen
private fun updateImageView(state: AppState) {
    val currentImage = state.getImage()
    if (currentImage != null) {
        imageLabel.icon = ImageIcon(currentImage)
    }
    mainFrame.pack()
}

fun createButtonPanel(state: AppState, infoPanel: InfoPanel): JPanel {
    colorDisplayPanel.background = state.getColor()
    colorDisplayPanel.border = BorderFactory.createLineBorder(Color.BLACK)
    colorDisplayPanel.preferredSize = Dimension(20, 20)

    // --- SLIDER DE BRILLO ---
    val brightnessSlider = JSlider(JSlider.HORIZONTAL, -100, 100, 0).apply {
        majorTickSpacing = 50
        minorTickSpacing = 10
        paintTicks = true
        paintLabels = true
        toolTipText = "Ajuste de Brillo (-1.0 a 1.0)"

        addChangeListener {
            // Solo aplicar cuando el usuario suelta el slider (mejora rendimiento)
            if (!this.valueIsAdjusting) {
                // Mapear valor de -100 a 100 a -1.0f a 1.0f
                val factor = this.value / 100f
                if (state.applyBrightness(factor)) {
                    updateImageView(state)
                }
            }
        }
    }
    val brightnessPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createTitledBorder("Brillo (Slider)")
        add(brightnessSlider, BorderLayout.CENTER)
        // Establecer un tamaño fijo para que no se estire demasiado horizontalmente
        preferredSize = Dimension(400, 70)
        maximumSize = Dimension(Int.MAX_VALUE, 70)
    }
    // ----------------------------


    val selectImageButton = JButton("Select Image").apply {
        addActionListener {
            val fileChooser = JFileChooser()
            val result = fileChooser.showOpenDialog(mainFrame)
            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedFile: File = fileChooser.selectedFile
                val metadata = state.loadImage(selectedFile)

                if (metadata != null) {
                    infoPanel.updateMetadata(metadata)
                    updateImageView(state)
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Error loading image. Check console.", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    }

    val applyGrayscaleButton = JButton("Apply Grayscale").apply {
        addActionListener {
            if (state.applyGrayscale()) {
                updateImageView(state)
                infoPanel.updateMetadata(state.getCurrentMetadata())
            } else {
                JOptionPane.showMessageDialog(mainFrame, "No image loaded to apply filter.", "Error", JOptionPane.WARNING_MESSAGE)
            }
        }
    }

    val applyNegativeButton = JButton("Negative").apply {
        addActionListener {
            if (state.applyNegative()) {
                updateImageView(state)
                infoPanel.updateMetadata(state.getCurrentMetadata())
            } else {
                JOptionPane.showMessageDialog(mainFrame, "No image loaded to apply filter.", "Error", JOptionPane.WARNING_MESSAGE)
            }
        }
    }

    val selectColorButton = JButton("Pick Color").apply {
        addActionListener {
            val newColor = JColorChooser.showDialog(
                mainFrame,
                "Choose Tint Color",
                state.getColor()
            )
            if (newColor != null) {
                state.setColor(newColor)
                colorDisplayPanel.background = newColor
            }
        }
    }

    val showHistogramButton = JButton("Show Histogram").apply {
        addActionListener {
            val histogramData = state.getHistogram()
            if (histogramData != null) {
                showHistogramWindow(histogramData)
            } else {
                JOptionPane.showMessageDialog(mainFrame, "No image loaded.", "No Image Selected", JOptionPane.WARNING_MESSAGE)
            }
        }
    }

    val showTonalCurveButton = JButton("Show Tonal Curve").apply {
        addActionListener {
            if (state.getImage() != null) {
                showTonalCurveWindow(state)
            } else {
                JOptionPane.showMessageDialog(mainFrame, "No image loaded or curve data unavailable.", "No Image Selected", JOptionPane.WARNING_MESSAGE)
            }
        }
    }

    // Panel de Controles Unificado
    val topButtonsPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        add(selectImageButton)
        add(applyNegativeButton)
        add(applyGrayscaleButton)
        add(selectColorButton)
        add(colorDisplayPanel)
        add(showHistogramButton)
        add(showTonalCurveButton)
    }

    // Contenedor principal para apilar botones y sliders
    return JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(topButtonsPanel)
        add(brightnessPanel)
        // Aquí podrías añadir el panel de contraste si lo reintroduces
    }
}

fun setupWindowDrag(frame: JFrame) {
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

fun showHistogramWindow(histogramData: Histogram) {
    val histogramFrame = JFrame("Histogram").apply {
        setSize(400, 300)
        // Asumo que HistogramPanel existe en org.pdi.ui
        add(org.pdi.ui.HistogramPanel(histogramData))
        isVisible = true
    }
    setupWindowDrag(histogramFrame)
}

fun showTonalCurveWindow(state: AppState) {
    val curveData = state.getTonalCurve()
    if (curveData == null) {
        JOptionPane.showMessageDialog(mainFrame, "Curve data is not available.", "Error", JOptionPane.ERROR_MESSAGE)
        return
    }

    val curveFrame = JFrame("Tonal Curve Viewer").apply {
        setSize(450, 500)
        layout = BorderLayout()
    }

    // Asumo que TonalCurvePanel existe en org.pdi.ui
    val curvePanel = org.pdi.ui.TonalCurvePanel(fullCurveData = curveData)

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

fun createAndShowGUI(state: AppState) {
    mainFrame = JFrame("Image Viewer").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(800, 600)
    }

    // Asumo que InfoPanel existe en org.pdi.ui
    val infoPanel = org.pdi.ui.InfoPanel()

    val mainPanel = JPanel(BorderLayout()).apply {
        add(createButtonPanel(state, infoPanel), BorderLayout.NORTH)
        add(infoPanel, BorderLayout.WEST)
        val imageScrollPane = JScrollPane(imageLabel)
        add(imageScrollPane, BorderLayout.CENTER)
    }

    mainFrame.contentPane.add(mainPanel)
    mainFrame.pack()
    mainFrame.isVisible = true
}