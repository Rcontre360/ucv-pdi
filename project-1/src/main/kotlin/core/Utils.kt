package org.pdi.core

import java.awt.Color
import kotlin.math.roundToInt

fun luminosity(c: Color):Int{
    return (0.2126 * c.red + 0.7152 * c.green + 0.0722 * c.blue).roundToInt()
}