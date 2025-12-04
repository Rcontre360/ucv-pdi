package org.pdi.ui

import org.pdi.core.Metadata
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class InfoPanel : JPanel() {
    private val widthLabel = JLabel("Width: ")
    private val heightLabel = JLabel("Height: ")
    private val bppLabel = JLabel("Bits Per Pixel: ")
    private val uniqueColorsLabel = JLabel("Unique Colors: ")
    private val formatLabel = JLabel("Format: ")

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createTitledBorder("Image Information")
        add(widthLabel)
        add(heightLabel)
        add(bppLabel)
        add(uniqueColorsLabel)
        add(formatLabel)
    }

    fun updateMetadata(metadata: Metadata?) {
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
}