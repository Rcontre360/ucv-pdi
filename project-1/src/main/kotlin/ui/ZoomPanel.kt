package org.pdi.ui

import org.pdi.core.AppState
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class ZoomPanel(private val state: AppState) : JPanel(FlowLayout()) {

    private val zoomLabel = JLabel("x1.0")
    private val zoomInButton = JButton("+")
    private val zoomOutButton = JButton("-")

    init {
        add(zoomOutButton)
        add(zoomLabel)
        add(zoomInButton)

        state.setOnZoomUpdateListener { factor: Float ->
            zoomLabel.text = "x${"%.1f".format(factor)}"
        }

        zoomInButton.addActionListener {
            state.zoomIn()
        }

        zoomOutButton.addActionListener {
            state.zoomOut()
        }
    }
}
