package org.pdi.ui

import javafx.fxml.FXML
import javafx.scene.control.TextField
import java.text.DecimalFormat

class ValueAdjusterController {

    @FXML
    private lateinit var valueField: TextField

    private var value: Float = 0.0f
    private var minValue: Float = 0.0f
    private var maxValue: Float = 0.0f
    private var step: Float = 0.0f
    private var onValueChange: ((Float) -> Unit)? = null

    private val decimalFormat = DecimalFormat("0.00")

    @FXML
    fun initialize() {
        valueField.textProperty().addListener { _, _, newValue ->
            try {
                val parsedValue = newValue.toFloat()
                if (parsedValue != value) {
                    value = parsedValue.coerceIn(minValue, maxValue)
                    onValueChange?.invoke(value)
                }
            } catch (e: NumberFormatException) {
                // Ignore invalid input for now
            }
        }
    }

    fun setup(initialValue: Float, minValue: Float, maxValue: Float, step: Float, onValueChange: (Float) -> Unit) {
        this.value = initialValue
        this.minValue = minValue
        this.maxValue = maxValue
        this.step = step
        this.onValueChange = onValueChange

        valueField.text = decimalFormat.format(value)
    }

    @FXML
    fun minus() {
        adjustValue(-step)
    }

    @FXML
    fun plus() {
        adjustValue(step)
    }

    private fun adjustValue(delta: Float) {
        value = (value + delta).coerceIn(minValue, maxValue)
        valueField.text = decimalFormat.format(value)
        onValueChange?.invoke(value)
    }

    fun setValue(newValue: Float) {
        value = newValue.coerceIn(minValue, maxValue)
        valueField.text = decimalFormat.format(value)
    }
}
