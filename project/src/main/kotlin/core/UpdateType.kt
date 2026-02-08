package org.pdi.core

import java.awt.Color
import java.io.File
import org.opencv.core.Point
import org.pdi.core.kernels.Kernel
import org.pdi.core.transforms.Transform

// this is a utility object using by the app state controller.
// it defines the different updates we can perform on the state app.
// each update has its own fields, thats why we used a sealed class with different
// objects definitions
sealed class UpdateType {
    // returns to the current original image
    object Clear : UpdateType()
    data class BrightnessUpdate(val newFactor: Float) : UpdateType()
    data class ContrastUpdate(val newFactor: Float) : UpdateType()
    // for grayscale we might be paiting the image with the given color
    data class GrayscaleUpdate(val tint: Color) : UpdateType()
    data class NegativeUpdate(val isNegative: Boolean) : UpdateType()
    data class RotationUpdate(val angle: Int) : UpdateType()
    // this is for binary thresholding (umbralization in the code, typo for mixing spanish/english)
    data class ThresholdUpdate(val type: Int) : UpdateType()
    object ZoomInUpdate : UpdateType()
    object ZoomOutUpdate : UpdateType()
    data class LoadImageUpdate(val file: File) : UpdateType()
    data class ConvolutionUpdate(val kernel: Kernel) : UpdateType()
    // border operations are different from convolution because we calculate the gradient
    data class BorderOperation(val kernelX: Kernel, val kernelY: Kernel) : UpdateType()
    data class RegionGrowingUpdate(
        val seeds: List<Point>,
        val maxDiff: Int,
        val connectivity: Int
    ) : UpdateType()
    data class PanningModeUpdate(val isPanning: Boolean) : UpdateType()
    data class TranslationUpdate(val dx: Int, val dy: Int) : UpdateType()

    // New HLS adjustment types
    data class HueAdjustment(val deltaHue: Int) : UpdateType()
    data class SaturationAdjustment(val deltaSaturation: Float) : UpdateType()
    data class LightnessAdjustment(val deltaLightness: Float) : UpdateType()

    // New YUV adjustment types
    data class YAdjustment(val newFactor: Float) : UpdateType()
    data class UAdjustment(val newFactor: Float) : UpdateType()
    data class VAdjustment(val newFactor: Float) : UpdateType()
    data class FrequencyFilter(val space: Transform, val filter: org.pdi.core.transforms.FrequencyFilter) : UpdateType()
    data class KMeansQuantization(val k: Int) : UpdateType()
    data class UniformQuantization(val bits: Int) : UpdateType()
    data class MedianCutQuantization(val k: Int) : UpdateType()
}
