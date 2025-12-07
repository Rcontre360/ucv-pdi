package org.pdi.core

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.pow
import kotlin.math.roundToInt

enum class KernelType {
    AVERAGE,
    MEDIAN,
    GAUSSIAN,
    CUSTOM,
    LAPLACIAN_GAUSSIAN,
    SOBEL_X,
    SOBEL_Y
}

abstract class Kernel(var rows: Int, var cols: Int) {
    var kernel: Array<FloatArray> = Array(rows) { FloatArray(cols) }
    var type: KernelType = KernelType.CUSTOM

    abstract fun execute(image: BufferedImage): BufferedImage

    fun setKernelValue(row: Int, col: Int, value: Float) {
        if (row < rows && col < cols) {
            kernel[row][col] = value
            type = KernelType.CUSTOM
        }
    }

    abstract fun generateKernel()

    fun applyKernel(other: Kernel): Kernel {
        val newKernel = object : LinearKernel(rows, cols) {
            override fun generateKernel() {
                // Not needed for this anonymous class
            }
        }

        val rowPad = other.rows / 2
        val colPad = other.cols / 2

        for (i in 0 until cols) {
            for (j in 0 until rows) {
                var sum = 0f
                for (k in 0 until other.cols) {
                    for (l in 0 until other.rows) {
                        val thisI = i - colPad + k
                        val thisJ = j - rowPad + l

                        if (thisI >= 0 && thisJ >= 0 && thisI < cols && thisJ < rows) {
                            sum += kernel[thisI][thisJ] * other.kernel[k][l]
                        }
                    }
                }
                newKernel.kernel[i][j] = sum
            }
        }
        return newKernel
    }
}

abstract class LinearKernel(rows: Int, cols: Int) : Kernel(rows, cols) {
    override fun execute(image: BufferedImage): BufferedImage {
        val newImage = BufferedImage(image.width, image.height, image.type)
        val kernelSum = kernel.sumOf { it.sum().toDouble() }.toFloat().let { if (it == 0f) 1f else it }

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                var r = 0f
                var g = 0f
                var b = 0f

                for (i in 0 until rows) {
                    for (j in 0 until cols) {
                        val imageX = (x - cols / 2 + j).coerceIn(0, image.width - 1)
                        val imageY = (y - rows / 2 + i).coerceIn(0, image.height - 1)
                        val color = Color(image.getRGB(imageX, imageY))
                        val kernelValue = kernel[i][j]

                        r += color.red * kernelValue
                        g += color.green * kernelValue
                        b += color.blue * kernelValue
                    }
                }

                val newColor = Color(
                    (r / kernelSum).roundToInt().coerceIn(0, 255),
                    (g / kernelSum).roundToInt().coerceIn(0, 255),
                    (b / kernelSum).roundToInt().coerceIn(0, 255)
                )
                newImage.setRGB(x, y, newColor.rgb)
            }
        }
        return newImage
    }
}


class MedianKernel(rows: Int, cols: Int) : Kernel(rows, cols) {

    override fun generateKernel() {
        this.type = KernelType.MEDIAN
        kernel = Array(rows) { FloatArray(cols) { 1f } }
    }

    override fun execute(image: BufferedImage): BufferedImage {
        val newImage = BufferedImage(image.width, image.height, image.type)

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rValues = mutableListOf<Int>()
                val gValues = mutableListOf<Int>()
                val bValues = mutableListOf<Int>()

                for (i in 0 until rows) {
                    for (j in 0 until cols) {
                        val imageX = (x - cols / 2 + j).coerceIn(0, image.width - 1)
                        val imageY = (y - rows / 2 + i).coerceIn(0, image.height - 1)
                        val color = Color(image.getRGB(imageX, imageY))
                        rValues.add(color.red)
                        gValues.add(color.green)
                        bValues.add(color.blue)
                    }
                }

                rValues.sort()
                gValues.sort()
                bValues.sort()

                val medianR = rValues[rValues.size / 2]
                val medianG = gValues[gValues.size / 2]
                val medianB = bValues[bValues.size / 2]

                newImage.setRGB(x, y, Color(medianR, medianG, medianB).rgb)
            }
        }
        return newImage
    }
}

class AverageKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    init {
        generateKernel()
    }

    override fun generateKernel() {
        this.type = KernelType.AVERAGE
        val centerX = cols / 2.0
        val centerY = rows / 2.0
        val radius = minOf(rows, cols) / 2.0

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val distance = kotlin.math.sqrt((i - centerY + 0.5).pow(2) + (j - centerX + 0.5).pow(2))
                if (distance <= radius) {
                    kernel[i][j] = 1f
                } else {
                    kernel[i][j] = 0f
                }
            }
        }
    }
}

class GaussianKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    private fun pascalRow(size: Int): FloatArray {
        if (size <= 0) return floatArrayOf()
        val n = size - 1
        val row = FloatArray(size)

        row[0] = 1.0f

        for (k in 1..n) {
            row[k] = row[k - 1] * (n.toFloat() - k.toFloat() + 1.0f) / k.toFloat()
        }
        return row
    }

    override fun generateKernel() {
        this.type = KernelType.GAUSSIAN
        val pascal1DRows = pascalRow(rows)
        val pascal1DCols = pascalRow(cols)
        var sum = 0.0f

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val value = pascal1DRows[i] * pascal1DCols[j]
                kernel[i][j] = value
                sum += value
            }
        }
    }
}

class DerivativeXKernel() : LinearKernel(3, 1) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.CUSTOM
        kernel = arrayOf(
            floatArrayOf(-1f, 0f, 1f)
        )
    }
}

class DerivativeYKernel() : LinearKernel(1, 3) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.CUSTOM
        kernel = arrayOf(
            floatArrayOf(-1f),
            floatArrayOf(0f),
            floatArrayOf(1f)
        )
    }
}

class LaplacianKernel : LinearKernel(3, 3) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.CUSTOM // Not visible to user
        kernel = arrayOf(
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(1f, -4f, 1f),
            floatArrayOf(0f, 1f, 0f)
        )
    }
}

class LaplacianGaussianKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.LAPLACIAN_GAUSSIAN
        val gaussian = GaussianKernel(rows, cols)
        gaussian.generateKernel()
        val laplacian = LaplacianKernel()
        val logKernel = gaussian.applyKernel(laplacian)
        this.kernel = logKernel.kernel
        this.rows = logKernel.rows
        this.cols = logKernel.cols
    }
}


class SobelXKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.SOBEL_X
        val gaussian = GaussianKernel(rows, cols)
        gaussian.generateKernel()
        val derivativeX = DerivativeXKernel()
        val sobelX = gaussian.applyKernel(derivativeX)
        this.kernel = sobelX.kernel
        this.rows = sobelX.rows
        this.cols = sobelX.cols
    }
}

class SobelYKernel(rows: Int, cols: Int) : LinearKernel(rows, cols) {
    init {
        generateKernel()
    }
    override fun generateKernel() {
        this.type = KernelType.SOBEL_Y
        val gaussian = GaussianKernel(rows, cols)
        gaussian.generateKernel()
        val derivativeY = DerivativeYKernel()
        val sobelY = gaussian.applyKernel(derivativeY)
        this.kernel = sobelY.kernel
        this.rows = sobelY.rows
        this.cols = sobelY.cols
    }
}