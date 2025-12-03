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

    fun getCurrentMetadata(): Metadata? {
        return _current?.getMetadata()
    }

    fun update() {
        println("AppState update called.")
    }

    fun getHistogram(): Map<Int, IntArray>? {
        return _current?.histogram
    }

    fun getImage(): BufferedImage? {
        return _current?.image
    }
}
