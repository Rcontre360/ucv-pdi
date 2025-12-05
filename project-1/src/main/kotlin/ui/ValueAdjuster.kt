package org.pdi.ui

import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ValueAdjuster(
    private var value: Float,
    private val minValue: Float,
    private val maxValue: Float,
    private val step: Float,
    private val onValueChange: (Float) -> Unit
) : JPanel() {

    private val valueField = JTextField(5)
    private val minusButton = JButton("-")
    private val plusButton = JButton("+")

    init {
        layout = FlowLayout(FlowLayout.LEFT, 2, 2) // Smaller gaps

        minusButton.addActionListener {
            adjustValue(-step)
        }

        plusButton.addActionListener {
            adjustValue(step)
        }

        valueField.text = String.format("%.2f", value)
        valueField.preferredSize = Dimension(50, 25) // Smaller text field
        valueField.horizontalAlignment = JTextField.CENTER
        valueField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateValueFromField()
            override fun removeUpdate(e: DocumentEvent?) = updateValueFromField()
            override fun changedUpdate(e: DocumentEvent?) = updateValueFromField()
        })

        add(minusButton)
        add(valueField)
        add(plusButton)
    }

    private fun adjustValue(delta: Float) {
        value = (value + delta).coerceIn(minValue, maxValue)
        valueField.text = String.format("%.2f", value)
        onValueChange(value)
    }

    private fun updateValueFromField() {
        try {
            val newValue = valueField.text.toFloat()
            if (newValue != value) {
                value = newValue.coerceIn(minValue, maxValue)
                onValueChange(value)
            }
        } catch (e: NumberFormatException) {
            // Ignore invalid input for now, or show an error
        }
    }

    fun setValue(newValue: Float) {
        value = newValue.coerceIn(minValue, maxValue)
        valueField.text = String.format("%.2f", value)
    }
}
