package org.pdi.core

import java.awt.Color
import kotlin.math.roundToInt

// a simple utility. Shared on other places and avoids bugs because of different formulas on different places
fun luminosity(c: Color):Int{
    return (0.2126 * c.red + 0.7152 * c.green + 0.0722 * c.blue).roundToInt()
}