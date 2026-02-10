package org.pdi.core.quantization

import org.opencv.core.Mat
import org.pdi.core.image.Image

abstract class Quantizer(val value: Int) {
    abstract fun apply(image: Image): Mat
}