package org.pdi.ui

import org.pdi.core.AppState
import java.awt.Color
import java.awt.Component
import java.io.File
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JFileChooser
import javax.swing.JOptionPane


fun createSelectColorButton(
    owner: Component,
    onColorSelected: (Color) -> Unit
): JButton {
    return JButton("Pick Color").apply {
        addActionListener {
            val newColor = JColorChooser.showDialog(
                owner,
                "Choose Tint Color",
                Color.WHITE
            )
            if (newColor != null) {
                onColorSelected(newColor)
            }
        }
    }
}
