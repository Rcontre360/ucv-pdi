package org.pdi.core

import java.awt.Color
import java.io.File

sealed class UpdateType {
    object Clear : UpdateType()
    data class BrightnessUpdate(val newFactor: Float) : UpdateType()
    data class ContrastUpdate(val newFactor: Float) : UpdateType()
    data class GrayscaleUpdate(val tint: Color) : UpdateType()
    data class NegativeUpdate(val isNegative: Boolean) : UpdateType()
    data class RotationUpdate(val angle: Int) : UpdateType()
    data class ThresholdUpdate(val thresholds: List<Int>) : UpdateType()
    object ZoomInUpdate : UpdateType()
    object ZoomOutUpdate : UpdateType()
    data class LoadImageUpdate(val file: File) : UpdateType()
    data class ColorUpdate(val color: Color) : UpdateType()
    data class ConvolutionUpdate(val kernel: Kernel) : UpdateType()
    data class BorderOperation(val kernelX: Kernel,val kernelY: Kernel) : UpdateType()
}
