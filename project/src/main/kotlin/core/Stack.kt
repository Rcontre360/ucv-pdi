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
    companion object {
        private const val MAX_HISTORY_SIZE = 10
    }

    private val history = mutableListOf<StateContext>()
    private var currentIndex = -1

    fun push(context: StateContext) {
        if (currentIndex < history.size - 1) {
            val statesToRemove = history.subList(currentIndex + 1, history.size)
            statesToRemove.clear()
        }

        history.add(context)
        currentIndex++

        // Si excedemos el límite, eliminamos el estado más antiguo y liberamos su memoria
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(0)
            currentIndex--
        }
    }

    fun updateCurrent(transform: (StateContext) -> StateContext) {
        val current = getCurrent() ?: StateContext()
        val newContext = transform(current)
        push(newContext)
    }

    fun undo(): StateContext? {
        println("UNDO ${currentIndex}")
        if (currentIndex > 0) {
            currentIndex--
            return history[currentIndex]
        }
        return null
    }

    fun redo(): StateContext? {
        println("REDO ${currentIndex}")
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
        // Liberamos absolutamente toda la memoria nativa antes de limpiar la lista
        history.forEach { it.currentImage?.close() }
        history.clear()
        currentIndex = -1
    }
}