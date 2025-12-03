package org.pdi

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import org.pdi.core.AppState
import org.pdi.core.Metadata

private val widthLabel = JLabel("Width: ")
private val heightLabel = JLabel("Height: ")
private val bppLabel = JLabel("Bits Per Pixel: ")
private val uniqueColorsLabel = JLabel("Unique Colors: ")
private val formatLabel = JLabel("Format: ")

private val imageLabel = JLabel()
private val colorDisplayPanel = JPanel()
private lateinit var mainFrame: JFrame

// -----------------------------------------------------------------

fun main(args: Array<String>) {
    val state = AppState()
    SwingUtilities.invokeLater {
        createAndShowGUI(state)
    }
}

fun updateMetadataDisplay(metadata: Metadata?) {
    if (metadata != null) {
        widthLabel.text = "Width: ${metadata.width}"
        heightLabel.text = "Height: ${metadata.height}"
        bppLabel.text = "Bits Per Pixel: ${metadata.bitsPerPixel}"
        uniqueColorsLabel.text = "Unique Colors: ${metadata.uniqueColors}"
        formatLabel.text = "Format: ${metadata.format}"
    } else {
        widthLabel.text = "Width: "
        heightLabel.text = "Height: "
        bppLabel.text = "Bits Per Pixel: "
        uniqueColorsLabel.text = "Unique Colors: "
        formatLabel.text = "Format: "
    }
}

fun createInfoPanel(): JPanel {
    val infoPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createTitledBorder("Image Information")
        add(widthLabel)
        add(heightLabel)
        add(bppLabel)
        add(uniqueColorsLabel)
        add(formatLabel)
    }
    return infoPanel
}

fun createButtonPanel(state: AppState): JPanel {
    colorDisplayPanel.background = state.getColor()
    colorDisplayPanel.border = BorderFactory.createLineBorder(Color.BLACK)
    colorDisplayPanel.preferredSize = Dimension(20, 20)

    val selectImageButton = JButton("Select Image").apply {
        addActionListener {
            val fileChooser = JFileChooser()
            val result = fileChooser.showOpenDialog(mainFrame)
            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedFile: File = fileChooser.selectedFile
                val metadata = state.loadImage(selectedFile)

                if (metadata != null) {
                    updateMetadataDisplay(metadata)
                    val currentImage = state.getImage()
                    if (currentImage != null) {
                        imageLabel.icon = ImageIcon(currentImage)
                    }
                    mainFrame.pack()
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Error loading image. Check console.", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    }

    val applyGrayscaleButton = JButton("Apply Grayscale").apply {
        addActionListener {
            if (state.applyGrayscale()) {
                val newImage = state.getImage()
                if (newImage != null) {
                    imageLabel.icon = ImageIcon(newImage)
                }
                updateMetadataDisplay(state.getCurrentMetadata())
                mainFrame.pack()
                println("Grayscale filter applied successfully.")
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
                JOptionPane.showMessageDialog(mainFrame, "Please select an image first.", "No Image Selected", JOptionPane.WARNING_MESSAGE)
            }
        }
    }

    return JPanel().apply {
        add(selectImageButton)
        add(applyGrayscaleButton)
        add(selectColorButton)
        add(colorDisplayPanel)
        add(showHistogramButton)
    }
}

fun showHistogramWindow(histogramData: Map<Int, IntArray>) {
    val histogramFrame = JFrame("Histogram").apply {
        setSize(400, 300)
        add(HistogramPanel(histogramData))
        isVisible = true
    }

    var initialClick: Point? = null
    histogramFrame.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            initialClick = e.point
            histogramFrame.contentPane.background = Color.LIGHT_GRAY
        }

        override fun mouseReleased(e: MouseEvent) {
            histogramFrame.contentPane.background = UIManager.getColor("Panel.background")
        }
    })

    histogramFrame.addMouseMotionListener(object : MouseAdapter() {
        override fun mouseDragged(e: MouseEvent) {
            val thisX = histogramFrame.location.x
            val thisY = histogramFrame.location.y
            val xMoved = e.x - initialClick!!.x
            val yMoved = e.y - initialClick!!.y
            histogramFrame.setLocation(thisX + xMoved, thisY + yMoved)
        }
    })
}

fun createAndShowGUI(state: AppState) {
    mainFrame = JFrame("Image Viewer").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(800, 600)
    }

    val mainPanel = JPanel(BorderLayout()).apply {
        add(createButtonPanel(state), BorderLayout.NORTH)

        add(createInfoPanel(), BorderLayout.WEST)

        val imageScrollPane = JScrollPane(imageLabel)
        add(imageScrollPane, BorderLayout.CENTER)
    }

    mainFrame.contentPane.add(mainPanel)
    mainFrame.pack()
    mainFrame.isVisible = true
}