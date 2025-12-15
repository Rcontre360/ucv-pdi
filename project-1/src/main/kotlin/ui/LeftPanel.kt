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
import javax.swing.JSlider

class LeftPanel(
    private val state: AppState,
) : JPanel() {

    private val colorDisplayPanel = JPanel()
    private lateinit var brightnessSlider: JSlider
    private lateinit var brightnessValueLabel: JLabel
    private lateinit var contrastSlider: JSlider
    private lateinit var contrastValueLabel: JLabel
    private var selectedColor: Color = Color.WHITE

    init {
        val infoPanel = InfoPanel(state)
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
            brightnessSlider = JSlider(JSlider.HORIZONTAL, -100, 100, 0).apply {
                majorTickSpacing = 50
                minorTickSpacing = 10
                paintTicks = true
                paintLabels = true
                addChangeListener {
                    val newValue = value / 100.0f
                    state.setBrightness(newValue)
                }
            }
            brightnessValueLabel = JLabel("Brightness: 0.00").apply {
                alignmentX = Component.LEFT_ALIGNMENT
            }
            add(brightnessValueLabel)
            add(brightnessSlider)

            // Contrast
            val contrastLabel = JLabel("Contrast:")
            contrastLabel.alignmentX = Component.LEFT_ALIGNMENT
            add(contrastLabel)
            contrastSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 0).apply {
                majorTickSpacing = 50
                minorTickSpacing = 10
                paintTicks = true
                paintLabels = true
                addChangeListener {
                    val newValue = value / 100.0f
                    state.setContrast(newValue)
                }
            }
            contrastValueLabel = JLabel("Contrast: 0.00").apply {
                alignmentX = Component.LEFT_ALIGNMENT
            }
            add(contrastValueLabel)
            add(contrastSlider)

            // Buttons
            val applyGrayscaleButton = createApplyGrayscaleButton(state, this) {
                selectedColor
            }.apply {
                text = "Grayscale"
                alignmentX = Component.LEFT_ALIGNMENT
            }
            val applyNegativeButton = createApplyNegativeButton(state, this).apply {
                alignmentX = Component.LEFT_ALIGNMENT
            }
            val selectColorButton = createSelectColorButton(state, this) { newColor ->
                selectedColor = newColor
                colorDisplayPanel.background = newColor
            }

            colorDisplayPanel.background = selectedColor
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
            brightnessSlider.value = (stateContext.brightness * 100).toInt()
            brightnessValueLabel.text = "Brightness: %.2f".format(stateContext.brightness)

            contrastSlider.value = (stateContext.contrast * 100).toInt()
            contrastValueLabel.text = "Contrast: %.2f".format(stateContext.contrast)
        }
    }
}
