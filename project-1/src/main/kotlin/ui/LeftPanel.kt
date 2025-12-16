package org.pdi.ui

import org.pdi.core.AppState
import org.pdi.core.StateContext
import org.pdi.ui.panels.InfoPanel
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSlider

class LeftPanel(
    private val state: AppState,
) : JPanel() {

    private val colorDisplayPanel = JPanel()
    private var brightnessSlider: JSlider
    private var brightnessValueLabel: JLabel
    private var contrastSlider: JSlider
    private var contrastValueLabel: JLabel
    private var selectedColor: Color = Color.WHITE

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        preferredSize = Dimension(200, 600)

        val infoPanel = InfoPanel(state)
        infoPanel.maximumSize = Dimension(Int.MAX_VALUE, infoPanel.preferredSize.height)
        infoPanel.alignmentX = Component.LEFT_ALIGNMENT
        add(infoPanel)

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

        val applyGrayscaleButton = JButton("Grayscale").apply {
            alignmentX = LEFT_ALIGNMENT
            addActionListener {
                state.applyGrayscale(selectedColor)
            }
        }


        val applyNegativeButton = JButton("Negative").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                state.applyNegative()
            }
        }

        val selectColorButton = JButton("Pick Color").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                val newColor = JColorChooser.showDialog(
                    this,
                    "Choose Tint Color",
                    Color.WHITE
                )
                if (newColor != null) {
                    selectedColor = newColor
                    colorDisplayPanel.background = newColor
                }
            }
        }

        colorDisplayPanel.background = selectedColor
        colorDisplayPanel.border = BorderFactory.createLineBorder(Color.BLACK)
        colorDisplayPanel.preferredSize = Dimension(20, 20)
        val colorSelectionPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(selectColorButton)
            add(colorDisplayPanel)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        add(applyGrayscaleButton)
        add(applyNegativeButton)
        add(colorSelectionPanel)

        state.addContextListener { stateContext: StateContext ->
            brightnessSlider.value = (stateContext.brightness * 100).toInt()
            brightnessValueLabel.text = "Brightness: %.2f".format(stateContext.brightness)

            contrastSlider.value = (stateContext.contrast * 100).toInt()
            contrastValueLabel.text = "Contrast: %.2f".format(stateContext.contrast)
        }
    }
}
