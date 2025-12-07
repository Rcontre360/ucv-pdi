package org.pdi.ui

import org.pdi.core.AppState
import org.pdi.core.StateContext
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class LeftPanel(
    private val state: AppState,
    private val infoPanel: InfoPanel
) : JPanel() {

    private val colorDisplayPanel = JPanel()
    private lateinit var brightnessAdjuster: ValueAdjuster
    private lateinit var contrastAdjuster: ValueAdjuster

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        preferredSize = Dimension(250, 600) // Set a preferred width and height for the left panel
        minimumSize = Dimension(250, 600)
        maximumSize = Dimension(250, Int.MAX_VALUE) // Allow vertical expansion but maintain width
        alignmentX = Component.LEFT_ALIGNMENT

        // Add InfoPanel
        infoPanel.maximumSize = Dimension(Int.MAX_VALUE, infoPanel.preferredSize.height) // Make infoPanel expand horizontally
        infoPanel.alignmentX = Component.LEFT_ALIGNMENT
        add(infoPanel)

        // Add other transformations
        val transformationsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            // border = BorderFactory.createTitledBorder("Transformations") // Removed border
            preferredSize = Dimension(250, 300) // Fixed width, reasonable preferred height
            minimumSize = Dimension(250, 300) // Fixed width, reasonable minimum height
            maximumSize = Dimension(250, 300) // Fixed width, fixed maximum height
            alignmentX = Component.LEFT_ALIGNMENT

            // Brightness
            val brightnessLabel = JLabel("Brightness:")
            brightnessLabel.alignmentX = Component.LEFT_ALIGNMENT
            add(brightnessLabel)
            brightnessAdjuster = ValueAdjuster(state.context.brightness, -1.0f, 1.0f, 0.01f) { newValue ->
                state.setBrightness(newValue)
            }
            brightnessAdjuster.alignmentX = Component.LEFT_ALIGNMENT
            add(brightnessAdjuster)

            // Contrast
            val contrastLabel = JLabel("Contrast:")
            contrastLabel.alignmentX = Component.LEFT_ALIGNMENT
            add(contrastLabel)
            contrastAdjuster = ValueAdjuster(state.context.contrast, 0.0f, 2.0f, 0.01f) { newValue ->
                state.setContrast(newValue)
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
                state.setColor(newColor)
            }

            colorDisplayPanel.background = state.context.color
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

            add(Box.createVerticalGlue()) // Push everything to the top
        }
        add(transformationsPanel)

        state.addContextListener { stateContext: StateContext ->
            brightnessAdjuster.setValue(stateContext.brightness)
            contrastAdjuster.setValue(stateContext.contrast)
            colorDisplayPanel.background = stateContext.color
        }
    }
}
