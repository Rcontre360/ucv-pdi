package org.pdi.core

import java.awt.Color
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class AppState {
    private var _original: Image? = null
    private var _current: Image? = null
    private var _color: Color = Color.white

    fun loadImage(file: File): Metadata? {
        try {
            _original = Image(ImageIO.read(file))
            _current = _original

            return _original!!.getMetadata()

        } catch (e: Exception) {
            e.printStackTrace()
            _original = null
            _current = null
            return null
        }
    }

    fun setColor(color: Color) {
        _color = color
    }

    fun getColor(): Color {
        return _color
    }

    fun applyGrayscale(): Boolean {
        val currentImage = _original ?: return false
        val grayscaleImage = currentImage.toGrayscale(_color)

        _current = grayscaleImage
        return true
    }

    fun applyNegative(): Boolean {
        val currentImage = _original ?: return false
        val negative = currentImage.negative()

        _current = negative
        return true
    }

    fun applyBrightness(factor:Float): Boolean {
        val currentImage = _original ?: return false
        val bright = currentImage.changeBrightness(factor)
        _current = bright
        return true
    }

    fun applyContrast(factor:Float): Boolean {
        val currentImage = _original ?: return false
        val contrast = currentImage.changeContrast(factor)
        _current = contrast
        return true
    }

    fun isCurrentImageGrayscale(): Boolean {
        return _current?.is_grayscale ?: false
    }

    fun applyThresholding(thresholds: List<Int>): Boolean {
        if (!isCurrentImageGrayscale()) {
            return false
        }
        val currentImage = _current ?: return false
        _current = currentImage.makeThreshold(thresholds.toTypedArray())
        return true
    }

    fun getTonalCurve(): List<Pair<Color, Color>>? {
        return _original?.let { originalImage ->
            _current?.let { currentImage ->
                originalImage.tonalCurve(currentImage)
            }
        }
    }

    fun getCurrentMetadata(): Metadata? {
        return _current?.getMetadata()
    }

    fun update() {
        println("AppState update called.")
    }

    fun getHistogram(): Histogram? {
        return _current?.histogram
    }

    fun getImage(): BufferedImage? {
        return _current?.image
    }
}
