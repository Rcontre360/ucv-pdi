package org.pdi.core

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.pdi.core.kernels.MedianKernel
import org.pdi.core.kernels.SobelXKernel
import org.pdi.core.kernels.SobelYKernel

class AppState {
    private var _initialImage: Image? = null
    private var _current: Image? = null
    var color: Color = Color.white
        private set

    // Transformation state
    var brightness: Float = 0.0f
        private set
    var contrast: Float = 0.0f
        private set
    var rotationApplied: Int = 0
        private set
    var isGrayscaleApplied: Boolean = false
        private set

    // Listeners
    private var onImageUpdate: ((BufferedImage?) -> Unit)? = null
    private var onMetadataUpdate: ((Metadata?) -> Unit)? = null
    private var onBrightnessUpdate: ((Float) -> Unit)? = null
    private var onContrastUpdate: ((Float) -> Unit)? = null
    private var onZoomUpdate: ((Float) -> Unit)? = null

    fun setOnImageUpdateListener(listener: (BufferedImage?) -> Unit) {
        onImageUpdate = listener
    }

    fun setOnMetadataUpdateListener(listener: (Metadata?) -> Unit) {
        onMetadataUpdate = listener
    }

    fun setOnBrightnessUpdateListener(listener: (Float) -> Unit) {
        onBrightnessUpdate = listener
    }

    fun setOnContrastUpdateListener(listener: (Float) -> Unit) {
        onContrastUpdate = listener
    }

    fun clear() {
        brightness = 0.0f
        contrast = 0.0f
        isGrayscaleApplied = false
        rotationApplied = 0
        _current = _initialImage
        notifyUpdates()
        onBrightnessUpdate?.invoke(brightness)
        onContrastUpdate?.invoke(contrast)
    }

    private fun notifyUpdates() {
        onImageUpdate?.invoke(_current?.image)
        onMetadataUpdate?.invoke(_current?.getMetadata())
    }

    fun setColor(color: Color) {
        this.color = color
    }

    fun applyGrayscale() {
        isGrayscaleApplied = true
        _current = _current?.toGrayscale(color)
        notifyUpdates()
    }

    fun applyNegative() {
        _current = _current?.negative()
        notifyUpdates()
    }

    fun setBrightness(newFactor: Float) {
        val oldFactor = this.brightness
        this.brightness = newFactor
        val adjustment = (1 + newFactor) / (1 + oldFactor) - 1
        _current = _current?.changeBrightness(adjustment)
        notifyUpdates()
    }

    fun setContrast(newFactor: Float) {
        val oldFactor = this.contrast
        this.contrast = newFactor
        val adjustment = (1 + newFactor) / (1 + oldFactor) - 1
        _current = _current?.changeContrast(adjustment)
        notifyUpdates()
    }

    fun rotate(angle: Int) {
        rotationApplied = (rotationApplied + angle) % 360
        _current = _current?.rotateStraight(angle)
        notifyUpdates()
    }

    fun isCurrentImageGrayscale(): Boolean {
        return isGrayscaleApplied
    }

    fun applyThresholding(thresholds: List<Int>): Boolean {
        if (!isCurrentImageGrayscale()) {
            return false
        }
        val imageToChange = _current ?: return false
        _current = imageToChange.makeThreshold(thresholds.toTypedArray())
        notifyUpdates()
        return true
    }

    var zoomAlgorithm: ZoomAlgorithm = ZoomAlgorithm.LINEAR_INTERPOLATION
    private val zoomLevels = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f)
    private var currentZoomLevelIndex = 9 // index for 1.0f

    fun setOnZoomUpdateListener(listener: (Float) -> Unit) {
        onZoomUpdate = listener
    }

    fun zoomIn() {
        if (currentZoomLevelIndex < zoomLevels.size - 1) {
            val oldFactor = zoomLevels[currentZoomLevelIndex]
            currentZoomLevelIndex++
            val newFactor = zoomLevels[currentZoomLevelIndex]
            val adjustment = newFactor / oldFactor
            _current = _current?.zoom(adjustment, zoomAlgorithm)
            notifyUpdates()
            onZoomUpdate?.invoke(newFactor)
        }
    }

    fun zoomOut() {
        if (currentZoomLevelIndex > 0) {
            val oldFactor = zoomLevels[currentZoomLevelIndex]
            currentZoomLevelIndex--
            val newFactor = zoomLevels[currentZoomLevelIndex]
            val adjustment = newFactor / oldFactor
            _current = _current?.zoom(adjustment, zoomAlgorithm)
            notifyUpdates()
            onZoomUpdate?.invoke(newFactor)
        }
    }

    fun loadImage(file: File) {
        try {
            val loadedImage = Image(ImageIO.read(file))
            _initialImage = loadedImage
            clear()
            currentZoomLevelIndex = 9
            onZoomUpdate?.invoke(zoomLevels[currentZoomLevelIndex])
        } catch (e: Exception) {
            e.printStackTrace()
            _initialImage = null
            _current = null
            notifyUpdates()
        }
    }

    fun getTonalCurve(): List<Pair<Color, Color>>? {
        return _initialImage?.let { initial ->
            _current?.let { current ->
                initial.tonalCurve(current)
            }
        }
    }

    fun getCurrentMetadata(): Metadata? {
        return _current?.getMetadata()
    }

    fun getHistogram(): Histogram? {
        return _current?.histogram
    }

    fun getImage(): BufferedImage? {
        return _current?.image
    }

    fun applyConvolution(kernel: Kernel) {
        _current = _current?.applyKernel(kernel)
        notifyUpdates()
    }

    fun applyOperation(operation: BorderDetection) {
        _current = _current?.let { operation.apply(it) }
        notifyUpdates()
    }
}
