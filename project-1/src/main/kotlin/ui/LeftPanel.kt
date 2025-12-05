package org.pdi.ui

import org.pdi.core.AppState
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class LeftPanel(
    private val state: AppState,
    private val infoPanel: InfoPanel
) : JPanel() {

    private val colorDisplayPanel = JPanel()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        preferredSize = Dimension(250, preferredSize.height) // Set a preferred width for the left panel
        alignmentX = Component.LEFT_ALIGNMENT

        // Add InfoPanel
        infoPanel.maximumSize = Dimension(Int.MAX_VALUE, infoPanel.preferredSize.height) // Make infoPanel expand horizontally
        infoPanel.alignmentX = Component.LEFT_ALIGNMENT
        add(infoPanel)

        // Add other transformations
        val transformationsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Transformations")
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height) // Make transformationsPanel expand horizontally
            alignmentX = Component.LEFT_ALIGNMENT

            // Brightness
            val brightnessLabel = JLabel("Brightness:")
            brightnessLabel.alignmentX = Component.LEFT_ALIGNMENT
            add(brightnessLabel)
            val brightnessAdjuster = ValueAdjuster(state.brightness, -1.0f, 1.0f, 0.01f) { newValue ->
                state.setBrightness(newValue)
            }
            state.setOnBrightnessUpdateListener { factor: Float ->
                brightnessAdjuster.setValue(factor)
            }
            brightnessAdjuster.alignmentX = Component.LEFT_ALIGNMENT
            add(brightnessAdjuster)

            // Contrast
            val contrastLabel = JLabel("Contrast:")
            contrastLabel.alignmentX = Component.LEFT_ALIGNMENT
            add(contrastLabel)
            val contrastAdjuster = ValueAdjuster(state.contrast, 0.0f, 2.0f, 0.01f) { newValue ->
                state.setContrast(newValue)
            }
            state.setOnContrastUpdateListener { factor: Float ->
                contrastAdjuster.setValue(factor)
            }
            contrastAdjuster.alignmentX = Component.LEFT_ALIGNMENT
            add(contrastAdjuster)

            // Buttons
            val applyGrayscaleButton = createApplyGrayscaleButton(state, this).apply {
                text = "Grayscale"
                alignmentX = Component.LEFT_ALIGNMENT
            }
            val applyNegativeButton = createApplyNegativeButton(state, this).apply {
                alignmentX = Component.LEFT_ALIGNMENT
            }
            val selectColorButton = createSelectColorButton(state, this) { newColor ->
                colorDisplayPanel.background = newColor
            }

            colorDisplayPanel.background = state.color
            colorDisplayPanel.border = BorderFactory.createLineBorder(Color.BLACK)
            colorDisplayPanel.preferredSize = Dimension(20, 20)

            add(applyGrayscaleButton)
            add(applyNegativeButton)

            val colorSelectionPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(selectColorButton)
                add(colorDisplayPanel)
                alignmentX = Component.LEFT_ALIGNMENT
            }
            add(colorSelectionPanel)
        }
        add(transformationsPanel)
    }
}
