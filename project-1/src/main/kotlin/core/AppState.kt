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

    var currentImage: Image? = null
        private set
    var color: Color = Color.white
        private set
    var brightness: Float = 0.0f
        private set
    var contrast: Float = 0.0f
        private set
    var rotationApplied: Int = 0
        private set
    var isGrayscaleApplied: Boolean = false
        private set

    // Listeners
    var onImageUpdate: ((Image?) -> Unit)? = null

    private var onBrightnessUpdate: ((Float) -> Unit)? = null
    private var onContrastUpdate: ((Float) -> Unit)? = null
    private var onZoomUpdate: ((Float) -> Unit)? = null

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
        currentImage = _initialImage
        onBrightnessUpdate?.invoke(brightness)
        onContrastUpdate?.invoke(contrast)
    }

    fun setColor(color: Color) {
        this.color = color
    }

    fun applyGrayscale() {
        isGrayscaleApplied = true
        currentImage = currentImage?.toGrayscale(color)
        onImageUpdate?.invoke(currentImage)
    }

    fun applyNegative() {
        currentImage = currentImage?.negative()
        onImageUpdate?.invoke(currentImage)
    }

    fun setBrightness(newFactor: Float) {
        val oldFactor = this.brightness
        this.brightness = newFactor
        val adjustment = (1 + newFactor) / (1 + oldFactor) - 1
        currentImage = currentImage?.changeBrightness(adjustment)
        onImageUpdate?.invoke(currentImage)
    }

    fun setContrast(newFactor: Float) {
        val oldFactor = this.contrast
        this.contrast = newFactor
        val adjustment = (1 + newFactor) / (1 + oldFactor) - 1
        currentImage = currentImage?.changeContrast(adjustment)
        onImageUpdate?.invoke(currentImage)
    }

    fun rotate(angle: Int) {
        rotationApplied = (rotationApplied + angle) % 360
        currentImage = currentImage?.rotateStraight(angle)
        onImageUpdate?.invoke(currentImage)
    }

    fun isCurrentImageGrayscale(): Boolean {
        return isGrayscaleApplied
    }

    fun applyThresholding(thresholds: List<Int>): Boolean {
        if (!isCurrentImageGrayscale()) {
            return false
        }
        val imageToChange = currentImage ?: return false
        currentImage = imageToChange.makeThreshold(thresholds.toTypedArray())
        onImageUpdate?.invoke(currentImage)
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
            currentImage = currentImage?.zoom(adjustment, zoomAlgorithm)
            onImageUpdate?.invoke(currentImage)
            onZoomUpdate?.invoke(newFactor)
        }
    }

    fun zoomOut() {
        if (currentZoomLevelIndex > 0) {
            val oldFactor = zoomLevels[currentZoomLevelIndex]
            currentZoomLevelIndex--
            val newFactor = zoomLevels[currentZoomLevelIndex]
            val adjustment = newFactor / oldFactor
            currentImage = currentImage?.zoom(adjustment, zoomAlgorithm)
            onImageUpdate?.invoke(currentImage)
            onZoomUpdate?.invoke(newFactor)
        }
    }

    fun loadImage(file: File) {
        try {
            val loadedImage = Image(ImageIO.read(file))
            _initialImage = loadedImage
            currentImage = loadedImage
            clear()
            currentZoomLevelIndex = 9
            onZoomUpdate?.invoke(zoomLevels[currentZoomLevelIndex])
            onImageUpdate?.invoke(currentImage)
        } catch (e: Exception) {
            e.printStackTrace()
            _initialImage = null
            currentImage = null
            onImageUpdate?.invoke(currentImage)
        }
    }

    fun getTonalCurve(): List<Pair<Color, Color>>? {
        return _initialImage?.let { initial ->
            currentImage?.let { current ->
                initial.tonalCurve(current)
            }
        }
    }

    fun getCurrentMetadata(): Metadata? {
        return currentImage?.metadata
    }

    fun getHistogram(): Histogram? {
        return currentImage?.histogram
    }

    fun getImage(): BufferedImage? {
        return currentImage?.image
    }

    fun applyConvolution(kernel: Kernel) {
        currentImage = currentImage?.applyKernel(kernel)
        onImageUpdate?.invoke(currentImage)
    }

    fun applyOperation(operation: BorderDetection) {
        currentImage = currentImage?.let { operation.apply(it) }
        onImageUpdate?.invoke(currentImage)
    }

    fun getLineProfile(axis: Char, lineNumber: Int, channel: Char): List<Pair<Int, Int>>? {
        return currentImage?.getLineProfile(axis, lineNumber, channel)
    }
}
