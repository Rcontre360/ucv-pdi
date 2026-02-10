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
    val lightnessFactor: Float = 0.0f
)

// stack allows the user to stack actions.
// we store the image on each action but have a max history limit to avoid crashing the memory
class Stack {
    companion object {
        private const val MAX_HISTORY_SIZE = 10
    }

    // history and current index used
    private val history = mutableListOf<StateContext>()
    private var currentIndex = -1

    fun getCurrent(): StateContext? {
        return if (currentIndex >= 0) history[currentIndex] else null
    }

    fun push(context: StateContext) {
        if (currentIndex < history.size - 1) {
            val statesToRemove = history.subList(currentIndex + 1, history.size)
            statesToRemove.clear()
        }

        history.add(context)
        currentIndex++

        // if we exceed the limit. remove oldest record and free memory
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(0)
            currentIndex--
        }
    }

    // this basically updates the current context given a transformation. cleaner way to add new state
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

    // used when loading a new image
    fun clear() {
        history.forEach { it.currentImage?.close() }
        history.clear()
        currentIndex = -1
    }
}