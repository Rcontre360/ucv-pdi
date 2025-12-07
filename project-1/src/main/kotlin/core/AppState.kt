package org.pdi.core

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

data class StateContext(
    val currentImage: Image? = null,
    val color: Color = Color.white,
    val brightness: Float = 0.0f,
    val contrast: Float = 0.0f,
    val rotationApplied: Int = 0,
    val currentZoomLevelIndex: Int = 9 // Default for 1.0f
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

    fun getTonalCurve(): List<Pair<Color, Color>>? {
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
        update(UpdateType.NegativeUpdate)
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


    fun applyThresholding(thresholds: List<Int>): Boolean {
        if (!isCurrentImageGrayscale()) {
            return false
        }
        update(UpdateType.ThresholdUpdate(thresholds))
        return true
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

    private fun update(updateType: UpdateType) {
        var newStateContext = context.copy()

        when (updateType) {
            is UpdateType.Clear -> {
                context = StateContext(currentImage = _initialImage)
            }
            is UpdateType.BrightnessUpdate -> {
                val oldFactor = newStateContext.brightness
                val newFactor = updateType.newFactor
                val adjustment = (1 + newFactor) / (1 + oldFactor) - 1
                newStateContext = newStateContext.copy(
                    brightness = newFactor,
                    currentImage = newStateContext.currentImage?.changeBrightness(adjustment)
                )
            }
            is UpdateType.ContrastUpdate -> {
                val oldFactor = newStateContext.contrast
                val newFactor = updateType.newFactor
                val adjustment = (1 + newFactor) / (1 + oldFactor) - 1
                newStateContext = newStateContext.copy(
                    contrast = newFactor,
                    currentImage = newStateContext.currentImage?.changeContrast(adjustment)
                )
            }
            is UpdateType.GrayscaleUpdate -> {
                newStateContext = newStateContext.copy(
                    currentImage = newStateContext.currentImage?.toGrayscale(updateType.tint)
                )
            }
            UpdateType.NegativeUpdate -> {
                newStateContext = newStateContext.copy(
                    currentImage = newStateContext.currentImage?.negative()
                )
            }
            is UpdateType.RotationUpdate -> {
                newStateContext = newStateContext.copy(
                    rotationApplied = (newStateContext.rotationApplied + updateType.angle) % 360,
                    currentImage = newStateContext.currentImage?.rotateStraight(updateType.angle)
                )
            }
            is UpdateType.ThresholdUpdate -> {
                if (!isCurrentImageGrayscale()) {
                    return 
                }
                val imageToChange = newStateContext.currentImage ?: return
                newStateContext = newStateContext.copy(
                    currentImage = imageToChange.makeThreshold(updateType.thresholds.toTypedArray())
                )
            }
            UpdateType.ZoomInUpdate -> {
                if (newStateContext.currentZoomLevelIndex < zoomLevels.size - 1) {
                    val oldFactor = zoomLevels[newStateContext.currentZoomLevelIndex]
                    val newZoomIndex = newStateContext.currentZoomLevelIndex + 1
                    val newFactor = zoomLevels[newZoomIndex]
                    val adjustment = newFactor / oldFactor
                    newStateContext = newStateContext.copy(
                        currentZoomLevelIndex = newZoomIndex,
                        currentImage = newStateContext.currentImage?.zoom(adjustment, zoomAlgorithm)
                    )
                }
            }
            UpdateType.ZoomOutUpdate -> {
                if (newStateContext.currentZoomLevelIndex > 0) {
                    val oldFactor = zoomLevels[newStateContext.currentZoomLevelIndex]
                    val newZoomIndex = newStateContext.currentZoomLevelIndex - 1
                    val newFactor = zoomLevels[newZoomIndex]
                    val adjustment = newFactor / oldFactor
                    newStateContext = newStateContext.copy(
                        currentZoomLevelIndex = newZoomIndex,
                        currentImage = newStateContext.currentImage?.zoom(adjustment, zoomAlgorithm)
                    )
                }
            }
            is UpdateType.LoadImageUpdate -> {
                try {
                    val loadedImage = Image(ImageIO.read(updateType.file))
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
                newStateContext = newStateContext.copy(
                    currentImage = newStateContext.currentImage?.applyKernel(updateType.kernel)
                )
            }
            is UpdateType.BorderOperation -> {
                newStateContext = newStateContext.copy(
                    currentImage = newStateContext.currentImage?.applyBorderOperator(updateType.kernelX,updateType.kernelY)
                )
            }
        }

        context = newStateContext
        _contextListeners.forEach { it.invoke(context) }
    }

}

