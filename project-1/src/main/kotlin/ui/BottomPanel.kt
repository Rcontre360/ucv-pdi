package org.pdi.ui

import org.pdi.core.AppState
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

class BottomPanel(private val state: AppState) : JPanel() {
    init {
        layout = FlowLayout(FlowLayout.LEFT) // Align components to the left
        border = BorderFactory.createTitledBorder("Geometrical Transformations")

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
    }
}
