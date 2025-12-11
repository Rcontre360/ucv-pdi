package org.pdi.ui

import org.pdi.core.AppState
import java.awt.Color
import java.awt.Component
import java.io.File
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JFileChooser
import javax.swing.JOptionPane

fun createApplyGrayscaleButton(
    state: AppState,
    owner: Component
): JButton {
    return JButton("Apply Grayscale").apply {
        addActionListener {
            state.applyGrayscale()
        }
    }
}

fun createApplyNegativeButton(
    state: AppState,
    owner: Component
): JButton {
    return JButton("Negative").apply {
        addActionListener {
            state.applyNegative()
        }
    }
}

fun createSelectColorButton(
    state: AppState,
    owner: Component,
    onColorSelected: (Color) -> Unit
): JButton {
    return JButton("Pick Color").apply {
        addActionListener {
            val newColor = JColorChooser.showDialog(
                owner,
                "Choose Tint Color",
                state.context.color
            )
            if (newColor != null) {
                state.setColor(newColor)
                onColorSelected(newColor)
            }
        }
    }
}
