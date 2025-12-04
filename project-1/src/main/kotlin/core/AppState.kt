package org.pdi.core

import java.awt.Color
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class AppState {
    private var _initialImage: Image? = null
    private var _current: Image? = null
    private val _history: ArrayDeque<Image> = ArrayDeque()
    private var _historyIndex: Int = -1
    private val HISTORY_LIMIT = 20

    private var _color: Color = Color.white

    fun loadImage(file: File): Metadata? {
        return try {
            val loadedImage = Image(ImageIO.read(file))
            _initialImage = loadedImage
            clear() // Set up the initial state
            loadedImage.getMetadata()
        } catch (e: Exception) {
            e.printStackTrace()
            _initialImage = null
            _current = null
            _history.clear()
            _historyIndex = -1
            null
        }
    }

    fun clear() {
        _history.clear()
        _historyIndex = -1
        _initialImage?.let {
            _history.add(it)
            _historyIndex = 0
            _current = it
        }
    }

    private fun _update(newImage: Image) {
        // Truncate future history if we branched off
        while (_history.lastIndex > _historyIndex) {
            _history.removeLast()
        }

        _history.add(newImage)
        _historyIndex++

        // Enforce history limit
        if (_history.size > HISTORY_LIMIT) {
            _history.removeFirst()
            _historyIndex-- // Adjust index since we removed from the start
        }

        _current = newImage
    }

    fun undo(): Boolean {
        if (_historyIndex > 0) {
            _historyIndex--
            _current = _history[_historyIndex]
            return true
        }
        return false
    }

    fun redo(): Boolean {
        if (_historyIndex < _history.lastIndex) {
            _historyIndex++
            _current = _history[_historyIndex]
            return true
        }
        return false
    }

    fun setColor(color: Color) {
        _color = color
    }

    fun getColor(): Color {
        return _color
    }

    fun applyGrayscale(): Boolean {
        val imageToChange = _current ?: return false
        val grayscaleImage = imageToChange.toGrayscale(_color)
        _update(grayscaleImage)
        return true
    }

    fun applyNegative(): Boolean {
        val imageToChange = _current ?: return false
        val negativeImage = imageToChange.negative()
        _update(negativeImage)
        return true
    }

    fun applyBrightness(factor: Float): Boolean {
        val imageToChange = _current ?: return false
        val brightImage = imageToChange.changeBrightness(factor)
        _update(brightImage)
        return true
    }

    fun applyContrast(factor: Float): Boolean {
        val imageToChange = _current ?: return false
        val contrastImage = imageToChange.changeContrast(factor)
        _update(contrastImage)
        return true
    }

    fun isCurrentImageGrayscale(): Boolean {
        return _current?.isGrayscale ?: false
    }

    fun applyThresholding(thresholds: List<Int>): Boolean {
        if (!isCurrentImageGrayscale()) {
            return false
        }
        val imageToChange = _current ?: return false
        val thresholdedImage = imageToChange.makeThreshold(thresholds.toTypedArray())
        _update(thresholdedImage)
        return true
    }

    fun rotate(angle:Int): Boolean {
        if (angle % 90 != 0) {
            return false
        }
        val imageToChange = _current ?: return false
        val rotated = imageToChange.rotateStraight(angle)
        _update(rotated)
        return true
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
}
