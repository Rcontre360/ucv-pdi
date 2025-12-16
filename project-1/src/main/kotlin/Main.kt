package org.pdi

import java.awt.BorderLayout
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import org.pdi.core.*
import org.pdi.ui.BottomPanel
import org.pdi.ui.FiltersPanel
import org.pdi.ui.panels.InfoPanel
import org.pdi.ui.LeftPanel
import org.pdi.ui.MainButtonsPanel
import org.pdi.ui.OperationsPanel
import java.awt.image.BufferedImage

private val imageLabel = JLabel()
private lateinit var mainFrame: JFrame

fun main(args: Array<String>) {
    val state = AppState()
    SwingUtilities.invokeLater {
        buildGUI(state)
    }
}

fun buildGUI(state: AppState) {
    mainFrame = JFrame("Image Viewer").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(800, 600)
    }

    state.addContextListener { stateContext: StateContext ->
        if (stateContext.currentImage != null){
            imageLabel.icon = stateContext.currentImage.image?.let { ImageIcon(it) }
        } else {
            imageLabel.icon = null
        }
    }

    imageLabel.addMouseListener(object : java.awt.event.MouseAdapter() {
        override fun mousePressed(e: java.awt.event.MouseEvent) {
            state.onImageClick(e.point)
        }
    })

    val topPanel = MainButtonsPanel(state)
    val leftPanel = LeftPanel(state)
    val bottomPanel = BottomPanel(state)
    val filtersPanel = FiltersPanel(state)
    val operationsPanel = OperationsPanel(state)

    val eastPanel = JPanel(BorderLayout()).apply {
        add(filtersPanel, BorderLayout.NORTH)
        add(operationsPanel, BorderLayout.CENTER)
    }

    val mainPanel = JPanel(BorderLayout()).apply {
        add(topPanel, BorderLayout.NORTH)
        add(leftPanel, BorderLayout.WEST)
        add(bottomPanel, BorderLayout.SOUTH)
        add(eastPanel, BorderLayout.EAST)
        val imageScrollPane = JScrollPane(imageLabel)
        add(imageScrollPane, BorderLayout.CENTER)
    }

    mainFrame.contentPane.add(mainPanel)
    mainFrame.pack()
    mainFrame.isVisible = true
}
