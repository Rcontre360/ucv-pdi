package org.pdi.ui

import org.pdi.core.AppState
import javax.swing.JSlider

fun createBrightnessSlider(state: AppState): JSlider {
    val slider = JSlider(JSlider.HORIZONTAL, -100, 100, (state.brightness * 100).toInt())
    slider.majorTickSpacing = 50
    slider.minorTickSpacing = 10
    slider.paintTicks = true
    slider.paintLabels = true
    slider.toolTipText = "Ajuste de Brillo (-1.0 a 1.0)"

    slider.addChangeListener {
        if (!slider.valueIsAdjusting) {
            val factor = slider.value / 100f
            state.setBrightness(factor)
        }
    }

    state.setOnBrightnessUpdateListener { factor: Float ->
        slider.value = (factor * 100).toInt()
    }

    return slider
}

fun createContrastSlider(state: AppState): JSlider {
    val slider = JSlider(JSlider.HORIZONTAL, -100, 100, (state.contrast * 100).toInt())
    slider.majorTickSpacing = 50
    slider.minorTickSpacing = 10
    slider.paintTicks = true
    slider.paintLabels = true
    slider.toolTipText = "Ajuste de Contraste (0.0 a 1.0)"

    slider.addChangeListener {
        if (!slider.valueIsAdjusting) {
            val factor = slider.value / 100f
            state.setContrast(factor)
        }
    }

    state.setOnContrastUpdateListener { factor: Float ->
        slider.value = (factor * 100).toInt()
    }

    return slider
}
