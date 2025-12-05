package org.pdi

import java.awt.BorderLayout
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import org.pdi.core.*
import org.pdi.ui.InfoPanel
import org.pdi.ui.MainButtonsPanel
import java.awt.image.BufferedImage

private val imageLabel = JLabel()
private lateinit var mainFrame: JFrame

fun main(args: Array<String>) {
    val state = AppState()
    SwingUtilities.invokeLater {
        createAndShowGUI(state)
    }
}

fun createAndShowGUI(state: AppState) {
    mainFrame = JFrame("Image Viewer").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(800, 600)
    }

    val infoPanel = InfoPanel()

    state.setOnImageUpdateListener { image: BufferedImage? ->
        imageLabel.icon = image?.let { ImageIcon(it) }
        mainFrame.pack()
    }
    state.setOnMetadataUpdateListener { metadata: Metadata? ->
        infoPanel.updateMetadata(metadata)
    }

    val buttonsPanel = MainButtonsPanel(state, infoPanel)

    val mainPanel = JPanel(BorderLayout()).apply {
        add(buttonsPanel, BorderLayout.NORTH)
        add(infoPanel, BorderLayout.WEST)
        val imageScrollPane = JScrollPane(imageLabel)
        add(imageScrollPane, BorderLayout.CENTER)
    }

    mainFrame.contentPane.add(mainPanel)
    mainFrame.pack()
    mainFrame.isVisible = true
}