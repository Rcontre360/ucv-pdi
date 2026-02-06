package org.pdi.core

import org.opencv.core.Mat
import java.awt.Color
import java.io.File
import org.opencv.core.Point
import org.pdi.core.image.Histogram
import org.pdi.core.image.Image
import org.pdi.core.image.ZoomAlgorithm
import org.pdi.core.kernels.Kernel

class AppState {
    private var _originalLoadedImage: Image? = null
    private var _currentProcessedBaseImage: Image? = null
    private val _contextListeners = mutableListOf<(StateContext) -> Unit>()

    private val stack = Stack()
    // single context exists only on stack. no duplicated data which is important
    val context: StateContext
        get() = stack.getCurrent() ?: StateContext()

    var zoomAlgorithm: ZoomAlgorithm = ZoomAlgorithm.LINEAR_INTERPOLATION
    val zoomLevels = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f)

    fun isCurrentImageGrayscale(): Boolean = stack.getCurrent()?.currentImage?.isGrayscale ?: false
    fun getHistogram(): Histogram? = stack.getCurrent()?.currentImage?.histogram
    fun getImage(): Mat? = stack.getCurrent()?.currentImage?.image

    fun getTonalCurve(): Map<Char, IntArray>? {
        return _currentProcessedBaseImage?.let { initial ->
            stack.getCurrent()?.currentImage?.let { current ->
                initial.getTonalCurve(current)
            }
        }
    }

    fun addContextListener(listener: (StateContext) -> Unit) {
        _contextListeners.add(listener)
    }

    fun undo() {
        stack.undo()?.let { notifyListeners(it) }
    }

    fun redo() {
        stack.redo()?.let { notifyListeners(it) }
    }

    // Facade functions
    fun applyConvolution(kernel: Kernel) = update(UpdateType.ConvolutionUpdate(kernel))
    fun applyBorderOperator(kernelX: Kernel, kernelY: Kernel) = update(UpdateType.BorderOperation(kernelX, kernelY))
    fun clear() = update(UpdateType.Clear)
    fun applyGrayscale(tint: Color) = update(UpdateType.GrayscaleUpdate(tint))
    fun applyNegative() = update(UpdateType.NegativeUpdate(!(stack.getCurrent()?.isNegative ?: false)))
    fun setBrightness(newFactor: Float) = update(UpdateType.BrightnessUpdate(newFactor))
    fun setContrast(newFactor: Float) = update(UpdateType.ContrastUpdate(newFactor))
    fun rotate(angle: Int) = update(UpdateType.RotationUpdate(angle))
    fun zoomIn() = update(UpdateType.ZoomInUpdate)
    fun zoomOut() = update(UpdateType.ZoomOutUpdate)
    fun loadImage(file: File) = update(UpdateType.LoadImageUpdate(file))
    fun applyThresholding(type: Int) = update(UpdateType.ThresholdUpdate(type))
    fun applyRegionGrowing(seeds: List<Point>, maxDiff: Int, connectivity: Int) = update(UpdateType.RegionGrowingUpdate(seeds, maxDiff, connectivity))
    fun setPanningMode(value: Boolean) = update(UpdateType.PanningModeUpdate(value))
    fun applyTranslation(dx: Int, dy: Int) = update(UpdateType.TranslationUpdate(dx, dy))
    fun adjustHue(newFactor: Int) = update(UpdateType.HueAdjustment(newFactor))
    fun adjustSaturation(newFactor: Float) = update(UpdateType.SaturationAdjustment(newFactor))
    fun adjustLightness(newFactor: Float) = update(UpdateType.LightnessAdjustment(newFactor))
    fun adjustY(newFactor: Float) = update(UpdateType.YAdjustment(newFactor))
    fun adjustU(newFactor: Float) = update(UpdateType.UAdjustment(newFactor))
    fun adjustV(newFactor: Float) = update(UpdateType.VAdjustment(newFactor))
    fun applyDFTFilter(filterType: FilterType, threshold: Double) = update(UpdateType.DFTFilter(filterType, threshold))
    fun applyKMeansQuantization(k: Int) = update(UpdateType.KMeansQuantization(k))
    fun applyUniformQuantization(bits: Int) = update(UpdateType.UniformQuantization(bits))
    fun applyMedianCutQuantization(k: Int) = update(UpdateType.MedianCutQuantization(k))

    private fun isContextDefault(ctx: StateContext): Boolean {
        val default = StateContext()
        return ctx.brightness == default.brightness &&
                ctx.contrast == default.contrast &&
                ctx.rotationApplied == default.rotationApplied &&
                ctx.currentZoomLevelIndex == default.currentZoomLevelIndex &&
                ctx.isNegative == default.isNegative &&
                ctx.isPanningMode == default.isPanningMode &&
                ctx.translationX == default.translationX &&
                ctx.translationY == default.translationY &&
                ctx.hueFactor == default.hueFactor &&
                ctx.saturationFactor == default.saturationFactor &&
                ctx.lightnessFactor == default.lightnessFactor &&
                ctx.yFactor == default.yFactor &&
                ctx.uFactor == default.uFactor &&
                ctx.vFactor == default.vFactor
    }

    private fun updateContextChanges(ctx: StateContext): Image? {
        val base = _currentProcessedBaseImage ?: return null
        var current = base

        if (ctx.isNegative)
            current = current.negative()
        if (ctx.contrast != 0.0f)
            current = current.changeContrast(ctx.contrast)
        if (ctx.brightness != 0.0f)
            current = current.changeBrightness(ctx.brightness)
        if (ctx.hueFactor != 0 || ctx.saturationFactor != 0.0f || ctx.lightnessFactor != 0.0f)
            current = current.applyHLSAdjustments(ctx.hueFactor, ctx.saturationFactor, ctx.lightnessFactor)
        if (ctx.yFactor != 0.0f || ctx.uFactor != 0.0f || ctx.vFactor != 0.0f)
            current = current.applyYUVAdjustments(ctx.yFactor, ctx.uFactor, ctx.vFactor)
        if (ctx.currentZoomLevelIndex != 9)
            current = current.zoom(zoomLevels[ctx.currentZoomLevelIndex], zoomAlgorithm)
        if (ctx.rotationApplied != 0)
            current = current.rotate(ctx.rotationApplied)
        if (ctx.translationX != 0 || ctx.translationY != 0)
            current = current.translate(ctx.translationX, ctx.translationY)

        return current
    }

    private fun update(updateType: UpdateType) {
        when (updateType) {
            is UpdateType.Clear -> {
                _currentProcessedBaseImage = _originalLoadedImage
                stack.updateCurrent { StateContext(currentImage = _originalLoadedImage) }
            }
            // Context-only updates (Deferred image processing)
            is UpdateType.BrightnessUpdate -> stack.updateCurrent { it.copy(brightness = updateType.newFactor) }
            is UpdateType.ContrastUpdate -> stack.updateCurrent {
                it.copy(contrast = updateType.newFactor)
            }
            is UpdateType.NegativeUpdate -> stack.updateCurrent { it.copy(isNegative = updateType.isNegative) }
            is UpdateType.RotationUpdate -> stack.updateCurrent { it.copy(rotationApplied = updateType.angle) }
            is UpdateType.ZoomInUpdate -> stack.updateCurrent {
                if (it.currentZoomLevelIndex < zoomLevels.size - 1) it.copy(currentZoomLevelIndex = it.currentZoomLevelIndex + 1) else it
            }
            is UpdateType.ZoomOutUpdate -> stack.updateCurrent {
                if (it.currentZoomLevelIndex > 0) it.copy(currentZoomLevelIndex = it.currentZoomLevelIndex - 1) else it
            }
            is UpdateType.PanningModeUpdate -> stack.updateCurrent { it.copy(isPanningMode = updateType.isPanning) }
            is UpdateType.TranslationUpdate -> stack.updateCurrent {
                it.copy(translationX = it.translationX + updateType.dx, translationY = it.translationY + updateType.dy)
            }
            is UpdateType.HueAdjustment -> stack.updateCurrent { it.copy(hueFactor = updateType.deltaHue) }
            is UpdateType.SaturationAdjustment -> stack.updateCurrent { it.copy(saturationFactor = updateType.deltaSaturation) }
            is UpdateType.LightnessAdjustment -> stack.updateCurrent { it.copy(lightnessFactor = updateType.deltaLightness) }
            is UpdateType.YAdjustment -> stack.updateCurrent { it.copy(yFactor = updateType.newFactor) }
            is UpdateType.UAdjustment -> stack.updateCurrent { it.copy(uFactor = updateType.newFactor) }
            is UpdateType.VAdjustment -> stack.updateCurrent { it.copy(vFactor = updateType.newFactor) }

            // Base Image updates (Reset context to show correct tonal curve)
            is UpdateType.GrayscaleUpdate -> {
                 _currentProcessedBaseImage = stack.getCurrent()?.currentImage?.toGrayscale(updateType.tint)
                stack.updateCurrent { StateContext(currentImage = _currentProcessedBaseImage) }
            }
            is UpdateType.ThresholdUpdate -> {
                if (!isCurrentImageGrayscale()) return
                _currentProcessedBaseImage = stack.getCurrent()?.currentImage?.makeThreshold(updateType.type)
                stack.updateCurrent { StateContext(currentImage = _currentProcessedBaseImage) }
            }
            is UpdateType.LoadImageUpdate -> {
                val loadedImage = org.pdi.io.loadImage(updateType.file)
                _originalLoadedImage = loadedImage
                _currentProcessedBaseImage = loadedImage
                stack.clear()
                stack.updateCurrent { StateContext(currentImage = loadedImage) }
            }
            is UpdateType.ConvolutionUpdate -> {
                _currentProcessedBaseImage = stack.getCurrent()?.currentImage?.applyKernel(updateType.kernel)
                stack.updateCurrent { StateContext(currentImage = _currentProcessedBaseImage) }
            }
            is UpdateType.BorderOperation -> {
                _currentProcessedBaseImage = stack.getCurrent()?.currentImage?.applyBorderOperator(updateType.kernelX, updateType.kernelY)
                stack.updateCurrent { StateContext(currentImage = _currentProcessedBaseImage) }
            }
            is UpdateType.RegionGrowingUpdate -> {
                _currentProcessedBaseImage = stack.getCurrent()?.currentImage?.regionGrowing(updateType.seeds, updateType.maxDiff, updateType.connectivity)
                stack.updateCurrent { StateContext(currentImage = _currentProcessedBaseImage) }
            }
            is UpdateType.DFTFilter -> {
                _currentProcessedBaseImage = stack.getCurrent()?.currentImage?.frequencyFilter(updateType.threshold, updateType.type == FilterType.HIGH_PASS)
                stack.updateCurrent { StateContext(currentImage = _currentProcessedBaseImage) }
            }
            is UpdateType.KMeansQuantization -> {
                _currentProcessedBaseImage = stack.getCurrent()?.currentImage?.kMeansQuantization(updateType.k)
                stack.updateCurrent { StateContext(currentImage = _currentProcessedBaseImage) }
            }
            is UpdateType.UniformQuantization -> {
                _currentProcessedBaseImage = stack.getCurrent()?.currentImage?.uniformQuantization(updateType.bits)
                stack.updateCurrent { StateContext(currentImage = _currentProcessedBaseImage) }
            }
            is UpdateType.MedianCutQuantization -> {
                _currentProcessedBaseImage = stack.getCurrent()?.currentImage?.medianCutQuantization(updateType.k)
                stack.updateCurrent { StateContext(currentImage = _currentProcessedBaseImage) }
            }
        }

        // Final image rendering based on updated context
        stack.getCurrent()?.let { currentCtx ->
            val renderedImage = if (isContextDefault(currentCtx)) _currentProcessedBaseImage else updateContextChanges(currentCtx)
            stack.updateCurrent { it.copy(currentImage = renderedImage) }
            notifyListeners(stack.getCurrent()!!)
        }
    }

    private fun notifyListeners(ctx: StateContext) {
        _contextListeners.forEach { it.invoke(ctx) }
    }
}
