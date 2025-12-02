package org.pdi

import java.awt.BorderLayout
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import org.pdi.Image
import org.pdi.ImageType
import org.pdi.HistogramPanel
import java.awt.Color
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

fun main(args: Array<String>) {
    SwingUtilities.invokeLater {
        createAndShowGUI()
    }
}

fun createAndShowGUI() {
    val frame = JFrame("Image Viewer")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(800, 600) // Set a default size

    val mainPanel = JPanel(BorderLayout())

    // Image Information Panel
    val infoPanel = JPanel()
    infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
    infoPanel.border = BorderFactory.createTitledBorder("Image Information")

    val widthLabel = JLabel("Width: ")
    val heightLabel = JLabel("Height: ")
    val bppLabel = JLabel("Bits Per Pixel: ")
    val uniqueColorsLabel = JLabel("Unique Colors: ")

    infoPanel.add(widthLabel)
    infoPanel.add(heightLabel)
    infoPanel.add(bppLabel)
    infoPanel.add(uniqueColorsLabel)

    mainPanel.add(infoPanel, BorderLayout.WEST)

    val helloButton = JButton("Hello World")
    helloButton.addActionListener {
        println("Hello, World!")
    }

    val imageLabel = JLabel()
    val imageScrollPane = JScrollPane(imageLabel)

    var currentImage: Image? = null // To store the currently loaded image

    val selectImageButton = JButton("Select Image")
    selectImageButton.addActionListener {
        val fileChooser = JFileChooser()
        val result = fileChooser.showOpenDialog(frame)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile: File = fileChooser.selectedFile
            try {
                currentImage = Image(selectedFile) // Use our Image class
                imageLabel.icon = ImageIcon(currentImage!!.image)

                widthLabel.text = "Width: ${currentImage!!.width}"
                heightLabel.text = "Height: ${currentImage!!.height}"
bppLabel.text = "Bits Per Pixel: ${currentImage!!.bitsPerPixel}"
                uniqueColorsLabel.text = "Unique Colors: ${currentImage!!.uniqueColors}"

                frame.pack()
            } catch (e: Exception) {
                e.printStackTrace()
                JOptionPane.showMessageDialog(frame, "Error loading image", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    val showHistogramButton = JButton("Show Histogram")
    showHistogramButton.addActionListener {
        if (currentImage != null) {
            val histogramFrame = JFrame("Histogram")
            histogramFrame.setSize(400, 300)
            histogramFrame.add(HistogramPanel(currentImage!!.histogram))
            histogramFrame.isVisible = true

            // Make the histogram window movable
            var initialClick: Point? = null
            histogramFrame.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    initialClick = e.point
                    histogramFrame.getComponent(0).background = Color.RED // Just to show it's active
                }

                override fun mouseReleased(e: MouseEvent) {
                    histogramFrame.getComponent(0).background = UIManager.getColor("Panel.background")
                }
            })

            histogramFrame.addMouseMotionListener(object : MouseAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    val thisX = histogramFrame.location.x
                    val thisY = histogramFrame.location.y
                    val xMoved = e.x - initialClick!!.x
                    val yMoved = e.y - initialClick!!.y
                    val newX = thisX + xMoved
                    val newY = thisY + yMoved
                    histogramFrame.setLocation(newX, newY)
                }
            })

        } else {
            JOptionPane.showMessageDialog(frame, "Please select an image first.", "No Image Selected", JOptionPane.WARNING_MESSAGE)
        }
    }

    val buttonPanel = JPanel()
    buttonPanel.add(helloButton)
    buttonPanel.add(selectImageButton)
    buttonPanel.add(showHistogramButton)

    mainPanel.add(buttonPanel, BorderLayout.NORTH)
    mainPanel.add(imageScrollPane, BorderLayout.CENTER)

    frame.contentPane.add(mainPanel)
    frame.pack()
    frame.isVisible = true
}

