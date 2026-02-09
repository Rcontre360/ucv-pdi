package org.pdi.core.quantization

import org.opencv.core.Mat
import org.pdi.core.image.Image
import org.pdi.core.image.putRGB
import java.awt.Color

class UniformQuantizer(bits: Int) : Quantizer(bits) {
    override fun apply(image: Image): Mat {
        if (value == 8) return image.image.clone()
        if (value <= 0) return Mat.zeros(image.image.size(), image.image.type())

        val levels = (1 shl value)
        val step = 256.0 / levels
        val result = Mat.zeros(image.image.size(), image.image.type())

        image.readAllPixels { x, y, color ->
            val b = ((color.blue / step).toInt() * step).toInt().coerceIn(0, 255)
            val g = ((color.green / step).toInt() * step).toInt().coerceIn(0, 255)
            val r = ((color.red / step).toInt() * step).toInt().coerceIn(0, 255)
            result.putRGB(x, y, Color(b, g, r))
        }
        return result
    }
}