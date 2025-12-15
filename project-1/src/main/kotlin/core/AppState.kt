package org.pdi.core

import java.awt.Color
import java.awt.image.BufferedImage
import org.pdi.io.loadImage
import java.io.File
import javax.imageio.ImageIO

data class StateContext(
    val currentImage: Image? = null,
    val color: Color = Color.white,
    val brightness: Float = 0.0f,
    val contrast: Float = 0.0f,
    val rotationApplied: Int = 0,
    val currentZoomLevelIndex: Int = 9, // Default for 1.0f
    val isNegative: Boolean = false
)

class AppState {
    private var _initialImage: Image? = null

    private val _contextListeners = mutableListOf<(StateContext) -> Unit>()

    var context: StateContext = StateContext()
        private set

    var zoomAlgorithm: ZoomAlgorithm = ZoomAlgorithm.LINEAR_INTERPOLATION
    val zoomLevels = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f)

    fun isCurrentImageGrayscale(): Boolean {
        return context.currentImage?.isGrayscale?:false
    }

    fun getTonalCurve(): Map<Char, IntArray>? {
        return _initialImage?.let { initial ->
            context.currentImage?.let { current ->
                initial.getTonalCurve(current)
            }
        }
    }

    fun getCurrentMetadata(): Metadata? {
        return context.currentImage?.metadata
    }

    fun getHistogram(): Histogram? {
        return context.currentImage?.histogram
    }

    fun getImage(): BufferedImage? {
        return context.currentImage?.image
    }

    fun getLineProfile(axis: Char, lineNumber: Int, channel: Char): List<Pair<Int, Int>>? {
        return context.currentImage?.getLineProfile(axis, lineNumber, channel)
    }

    fun addContextListener(listener: (StateContext) -> Unit) {
        _contextListeners.add(listener)
    }

    fun applyConvolution(kernel: Kernel) {
        update(UpdateType.ConvolutionUpdate(kernel))
    }

    fun applyBorderOperator(kernelX:Kernel,kernelY:Kernel) {
        update(UpdateType.BorderOperation(kernelX,kernelY))
    }

    fun clear() {
        update(UpdateType.Clear)
    }

    fun setColor(color: Color) {
        update(UpdateType.ColorUpdate(context.color))
    }

    fun applyGrayscale() {
        update(UpdateType.GrayscaleUpdate(context.color))
    }

    fun applyNegative() {
        update(UpdateType.NegativeUpdate(!context.isNegative))
    }

    fun setBrightness(newFactor: Float) {
        update(UpdateType.BrightnessUpdate(newFactor))
    }

    fun setContrast(newFactor: Float) {
        update(UpdateType.ContrastUpdate(newFactor))
    }

    fun rotate(angle: Int) {
        update(UpdateType.RotationUpdate(angle))
    }

    fun zoomIn() {
        update(UpdateType.ZoomInUpdate)
    }

    fun zoomOut() {
        update(UpdateType.ZoomOutUpdate)
    }

    fun loadImage(file: File) {
        update(UpdateType.LoadImageUpdate(file))
    }

    fun applyThresholding(thresholds: List<Int>) {
        update(UpdateType.ThresholdUpdate(thresholds))
    }

    
    private fun isContextDefault(context: StateContext): Boolean {
        val default = StateContext()
        return context.brightness == default.brightness &&
                context.contrast == default.contrast &&
                context.color == default.color &&
                context.rotationApplied == default.rotationApplied &&
                context.currentZoomLevelIndex == default.currentZoomLevelIndex &&
                context.isNegative == default.isNegative
    }

    private fun updateContextChanges(context: StateContext): Image? {
        if (_initialImage == null) return null

        var image = _initialImage!!

        if (context.rotationApplied != 0) {
            image = image.rotateStraight(context.rotationApplied)
        }
        if (context.currentZoomLevelIndex != 9) {
            val factor = zoomLevels[context.currentZoomLevelIndex]
            image = image.zoom(factor, zoomAlgorithm)
        }
        if (context.color != Color.white) {
            image = image.toGrayscale(context.color)
        }
        if (context.brightness != 0.0f) {
            image = image.changeBrightness(context.brightness)
        }
        if (context.contrast != 0.0f) {
            image = image.changeContrast(context.contrast)
        }
        if (context.isNegative) {
            image = image.negative()
        }

        return image
    }

    private fun update(updateType: UpdateType) {
        var newStateContext = context.copy()

        when (updateType) {
            is UpdateType.Clear -> {
                newStateContext = StateContext(currentImage = _initialImage)
            }
            is UpdateType.BrightnessUpdate -> {
                newStateContext = newStateContext.copy(brightness = updateType.newFactor)
            }
            is UpdateType.ContrastUpdate -> {
                newStateContext = newStateContext.copy(contrast = updateType.newFactor)
            }
            is UpdateType.GrayscaleUpdate -> {
                _initialImage = _initialImage?.toGrayscale(updateType.tint)
                newStateContext = StateContext(currentImage = _initialImage)
            }
            is UpdateType.NegativeUpdate -> {
                newStateContext = newStateContext.copy(isNegative = updateType.isNegative)
            }
            is UpdateType.RotationUpdate -> {
                newStateContext = newStateContext.copy(
                    rotationApplied = (newStateContext.rotationApplied + updateType.angle) % 360
                )
            }
            is UpdateType.ThresholdUpdate -> {
                if (!isCurrentImageGrayscale()) {
                    return
                }
                _initialImage = _initialImage?.makeThreshold(updateType.thresholds.toTypedArray())
                newStateContext = StateContext(currentImage = _initialImage)
            }
            UpdateType.ZoomInUpdate -> {
                if (newStateContext.currentZoomLevelIndex < zoomLevels.size - 1) {
                    newStateContext = newStateContext.copy(
                        currentZoomLevelIndex = newStateContext.currentZoomLevelIndex + 1
                    )
                }
            }
            UpdateType.ZoomOutUpdate -> {
                if (newStateContext.currentZoomLevelIndex > 0) {
                    newStateContext = newStateContext.copy(
                        currentZoomLevelIndex = newStateContext.currentZoomLevelIndex - 1
                    )
                }
            }
            is UpdateType.LoadImageUpdate -> {
                try {
                    val loadedImage = Image(org.pdi.io.loadImage(updateType.file))
                    _initialImage = loadedImage
                    newStateContext = StateContext(currentImage = loadedImage)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _initialImage = null
                    newStateContext = StateContext() // Reset all state on error
                }
            }
            is UpdateType.ColorUpdate -> {
                newStateContext = newStateContext.copy(color = updateType.color)
            }
            is UpdateType.ConvolutionUpdate -> {
                _initialImage = _initialImage?.applyKernel(updateType.kernel)
                newStateContext = StateContext(currentImage = _initialImage)
            }
            is UpdateType.BorderOperation -> {
                _initialImage = _initialImage?.applyBorderOperator(updateType.kernelX,updateType.kernelY)
                newStateContext = StateContext(currentImage = _initialImage)
            }
        }

        if (!isContextDefault(newStateContext)) {
            newStateContext = newStateContext.copy(currentImage = updateContextChanges(newStateContext))
        }

        context = newStateContext
        _contextListeners.forEach { it.invoke(context) }
    }
}
