package org.pdi.core

data class StateContext(
    val currentImage: org.pdi.core.image.Image? = null,
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

class Stack {
    private val history = mutableListOf<StateContext>()
    private var currentIndex = -1

    fun push(context: StateContext) {
        if (currentIndex < history.size - 1) {
            history.subList(currentIndex + 1, history.size).clear()
        }
        history.add(context)
        currentIndex++
    }

    fun updateCurrent(transform: (StateContext) -> StateContext) {
        val current = getCurrent() ?: StateContext()
        val newContext = transform(current)
        push(newContext)
    }

    fun undo(): StateContext? {
        if (currentIndex > 0) {
            currentIndex--
            return history[currentIndex]
        }
        return null
    }

    fun redo(): StateContext? {
        if (currentIndex < history.size - 1) {
            currentIndex++
            return history[currentIndex]
        }
        return null
    }

    fun getCurrent(): StateContext? {
        return if (currentIndex >= 0) history[currentIndex] else null
    }

    fun clear() {
        history.clear()
        currentIndex = -1
    }
}