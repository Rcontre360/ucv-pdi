package org.pdi.core

import org.opencv.core.Mat
import java.awt.Color
import java.io.File
import org.opencv.core.Point

// state context. this class holds values that are used for transition between the initial image and the current one
// how we decided which values should go here? I wanted those that modify the tonal curve and those that dont interfere with the first ones
// the ones I want to show in the tonal curve are brightness, contrast and negative. The transformations that dont affect these ones are all
// geometric transformations
data class StateContext(
    val currentImage: Image? = null,
    val brightness: Float = 0.0f,
    val contrast: Float = 0.0f,
    val rotationApplied: Int = 0,
    val currentZoomLevelIndex: Int = 9,
    val isNegative: Boolean = false,
    val isPanningMode: Boolean = false,
    val translationX: Int = 0,
    val translationY: Int = 0,
    val hueFactor: Int = 0,
    val saturationFactor: Float = 0.0f,
    val lightnessFactor: Float = 0.0f,
    val yFactor: Float = 0.0f,
    val uFactor: Float = 0.0f,
    val vFactor: Float = 0.0f
)

class AppState {
    // initial image. It changes if we apply one way functions
    private var _originalLoadedImage: Image? = null
    private var _currentProcessedBaseImage: Image? = null

    // context listeners. Used on some parts of the UI. Helps listen for image changes
    private val _contextListeners = mutableListOf<(StateContext) -> Unit>()

    // context variables
    var context: StateContext = StateContext()
        private set

    // zoom related variables
    var zoomAlgorithm: ZoomAlgorithm = ZoomAlgorithm.LINEAR_INTERPOLATION
    val zoomLevels = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f)

    fun isCurrentImageGrayscale(): Boolean {
        return context.currentImage?.isGrayscale?:false
    }

    // returns tonal curve. From the src image to the target
    // it breaks when we apply geometrical transformations since the current image
    // will not have a 1-1 relationship on x,y from the initial image to the current one
    fun getTonalCurve(): Map<Char, IntArray>? {
        return _currentProcessedBaseImage?.let { initial ->
            context.currentImage?.let { current ->
                initial.getTonalCurve(current)
            }
        }
    }

    // I wont comment on this self explanatory functions..
    fun getHistogram(): Histogram? {
        return context.currentImage?.histogram
    }

    fun getImage(): Mat? {
        return context.currentImage?.image
    }

    // used on the UI to add a function to listen for the context updates
    fun addContextListener(listener: (StateContext) -> Unit) {
        _contextListeners.add(listener)
    }

    // functions below this one call the "update" function with some object defining the update
    fun applyConvolution(kernel: Kernel) {
        update(UpdateType.ConvolutionUpdate(kernel))
    }

    fun applyBorderOperator(kernelX:Kernel,kernelY:Kernel) {
        update(UpdateType.BorderOperation(kernelX,kernelY))
    }

    fun clear() {
        update(UpdateType.Clear)
    }

    fun applyGrayscale(tint: Color) {
        update(UpdateType.GrayscaleUpdate(tint))
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

    fun applyRegionGrowing(seeds: List<Point>, maxDiff: Int, connectivity: Int) {
        update(UpdateType.RegionGrowingUpdate(seeds, maxDiff, connectivity))
    }

    fun setPanningMode(value: Boolean) {
        update(UpdateType.PanningModeUpdate(value))
    }

    fun applyTranslation(dx: Int, dy: Int) {
        update(UpdateType.TranslationUpdate(dx, dy))
    }

    fun adjustHue(newFactor: Int) {
        update(UpdateType.HueAdjustment(newFactor))
    }

    fun adjustSaturation(newFactor: Float) {
        update(UpdateType.SaturationAdjustment(newFactor))
    }

    fun adjustLightness(newFactor: Float) {
        update(UpdateType.LightnessAdjustment(newFactor))
    }

    fun adjustY(newFactor: Float) {
        update(UpdateType.YAdjustment(newFactor))
    }

    fun adjustU(newFactor: Float) {
        update(UpdateType.UAdjustment(newFactor))
    }

    fun adjustV(newFactor: Float) {
        update(UpdateType.VAdjustment(newFactor))
    }

    fun applyDFT() {
        update(UpdateType.DFTUpdate)
    }

    // check if the context has changed. If so we will do other thigns on he update function..
    private fun isContextDefault(context: StateContext): Boolean {
        val default = StateContext()
        return context.brightness == default.brightness &&
                context.contrast == default.contrast &&
                context.rotationApplied == default.rotationApplied &&
                context.currentZoomLevelIndex == default.currentZoomLevelIndex &&
                context.isNegative == default.isNegative &&
                context.isPanningMode == default.isPanningMode &&
                context.translationX == default.translationX &&
                context.translationY == default.translationY &&
                context.hueFactor == default.hueFactor &&
                context.saturationFactor == default.saturationFactor &&
                context.lightnessFactor == default.lightnessFactor &&
                context.yFactor == default.yFactor &&
                context.uFactor == default.uFactor &&
                context.vFactor == default.vFactor
    }

    // this function takes the initial image and applies those functions that are not one way
    // this is debatible since an increase in brightness can be one way (loss of info)
    // so lets say I choose arbitrarily which functions I want to apply here.
    private fun updateContextChanges(context: StateContext): Image? {
        if (_currentProcessedBaseImage == null) return null
        // we apply each operation in an order that makes sense
        // for example makes sense to first apply negative, if we apply negative after brightness
        // an image that is negative will appear darker with a brightness increase
        // we dont know the effects of the order between contrast and brightness so we leave it arbitrarily
        // geometric transformations go later since these dont affect the previous ones
        var image = _currentProcessedBaseImage!!
        if (context.isNegative) {
            image = image.negative()
        }
        // I noticed that is also important to change contrast first and THEN change brightness.
        // if its not done in this order it starts changing colors in a wrong way
        if (context.contrast != 0.0f) {
            image = image.changeContrast(context.contrast)
        }
        if (context.brightness != 0.0f) {
            image = image.changeBrightness(context.brightness)
        }
        if (context.hueFactor != 0 || context.saturationFactor != 0.0f || context.lightnessFactor != 0.0f) {
            image = image.applyHLSAdjustments(context.hueFactor, context.saturationFactor, context.lightnessFactor)
        }
        if (context.yFactor != 0.0f || context.uFactor != 0.0f || context.vFactor != 0.0f) {
            image = image.applyYUVAdjustments(context.yFactor, context.uFactor, context.vFactor)
        }
        if (context.currentZoomLevelIndex != 9) {
            val factor = zoomLevels[context.currentZoomLevelIndex]
            image = image.zoom(factor, zoomAlgorithm)
        }
        if (context.rotationApplied != 0) {
            image = image.rotate(context.rotationApplied)
        }
        if (context.translationX != 0 || context.translationY != 0) {
            image = image.translate(context.translationX, context.translationY)
        }
        return image
    }

    // update the internal state. Any image modification will go through this function
    // this is useful since it allows us to change the image in x way when we made y change
    // this centralized design allows for complex changes too (in the future)
    private fun update(updateType: UpdateType) {
        var newStateContext = context.copy()
        when (updateType) {
            is UpdateType.Clear -> {
                _currentProcessedBaseImage = _originalLoadedImage
                newStateContext = StateContext(currentImage = _originalLoadedImage)
            }
            // START ---------------
            // from here we dont update the image, we just update the variables of the context object
            is UpdateType.BrightnessUpdate -> {
                newStateContext = newStateContext.copy(brightness = updateType.newFactor)
            }
            is UpdateType.ContrastUpdate -> {
                newStateContext = newStateContext.copy(contrast = updateType.newFactor)
            }
            is UpdateType.NegativeUpdate -> {
                newStateContext = newStateContext.copy(isNegative = updateType.isNegative)
            }
            is UpdateType.RotationUpdate -> {
                newStateContext = newStateContext.copy(
                    rotationApplied = updateType.angle
                )
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
            is UpdateType.PanningModeUpdate -> {
                newStateContext = newStateContext.copy(isPanningMode = updateType.isPanning)
            }
            is UpdateType.TranslationUpdate -> {
                newStateContext = newStateContext.copy(
                    translationX = context.translationX + updateType.dx,
                    translationY = context.translationY + updateType.dy
                )
            }
            // New HLS adjustment types
            is UpdateType.HueAdjustment -> {
                newStateContext = newStateContext.copy(hueFactor = updateType.deltaHue)
            }
            is UpdateType.SaturationAdjustment -> {
                newStateContext = newStateContext.copy(saturationFactor = updateType.deltaSaturation)
            }
            is UpdateType.LightnessAdjustment -> {
                newStateContext = newStateContext.copy(lightnessFactor = updateType.deltaLightness)
            }
            // New YUV adjustment types
            is UpdateType.YAdjustment -> {
                newStateContext = newStateContext.copy(yFactor = updateType.newFactor)
            }
            is UpdateType.UAdjustment -> {
                newStateContext = newStateContext.copy(uFactor = updateType.newFactor)
            }
            is UpdateType.VAdjustment -> {
                newStateContext = newStateContext.copy(vFactor = updateType.newFactor)
            }
            // END -------
            // all the operations below update _initialImage.
            // This means that once these operations are done we cant go back
            // we didnt took in account how this operations modify the tonal curve
            // thats why we decided to just change the initial image, to show the tonal
            // curve as it is f(x) = x
            is UpdateType.GrayscaleUpdate -> {
                _currentProcessedBaseImage = context.currentImage?.toGrayscale(updateType.tint)
                newStateContext = StateContext(currentImage = _currentProcessedBaseImage)
            }
            // thresholding (umbralizacion)
            is UpdateType.ThresholdUpdate -> {
                if (!isCurrentImageGrayscale()) {
                    return
                }
                _currentProcessedBaseImage = context.currentImage?.makeThreshold(updateType.thresholds.toTypedArray())
                newStateContext = StateContext(currentImage = _currentProcessedBaseImage)
            }
            // load an image
            is UpdateType.LoadImageUpdate -> {
                try {
                    val loadedImage = org.pdi.io.loadImage(updateType.file)
                    _originalLoadedImage = loadedImage
                    _currentProcessedBaseImage = loadedImage
                    newStateContext = StateContext(currentImage = loadedImage)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _originalLoadedImage = null
                    _currentProcessedBaseImage = null
                    newStateContext = StateContext() // Reset all state on error
                }
            }
            is UpdateType.ConvolutionUpdate -> {
                _currentProcessedBaseImage = context.currentImage?.applyKernel(updateType.kernel)
                newStateContext = StateContext(currentImage = _currentProcessedBaseImage)
            }
            is UpdateType.BorderOperation -> {
                _currentProcessedBaseImage = context.currentImage?.applyBorderOperator(updateType.kernelX,updateType.kernelY)
                newStateContext = StateContext(currentImage = _currentProcessedBaseImage)
            }
            is UpdateType.RegionGrowingUpdate -> {
                _currentProcessedBaseImage = context.currentImage?.regionGrowing(
                    updateType.seeds,
                    updateType.maxDiff,
                    updateType.connectivity
                )
                newStateContext = StateContext(currentImage = _currentProcessedBaseImage)
            }
            UpdateType.DFTUpdate -> {
            }
        }

        // if the context has changed we apply the context changes
        if (_currentProcessedBaseImage != null && !isContextDefault(newStateContext)) {
            newStateContext = newStateContext.copy(currentImage = updateContextChanges(newStateContext))
        } else if (_currentProcessedBaseImage != null && isContextDefault(newStateContext)) {
            newStateContext = newStateContext.copy(currentImage = _currentProcessedBaseImage)
        }

        context = newStateContext
        // we update anything we need to update on the UI with the calllbacks
        _contextListeners.forEach { it.invoke(context) }
    }
}
