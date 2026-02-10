package org.pdi.core.rg

import org.opencv.core.*
import org.pdi.core.image.euclidean
import org.pdi.core.image.getRGB
import org.pdi.core.image.putRGB
import org.pdi.core.image.randomColor
import java.awt.Color
import java.util.*

abstract class RegionGrowing(val seed: Point, val threshold: Double, val connectivity: Int) {

    abstract fun shouldInclude(neighborColor: Color, seedColor: Color): Boolean

    open fun onPixelAdded(pixelColor: Color) {}

    fun execute(image: Mat): Mat {
        val height = image.rows()
        val width = image.cols()
        val resultMat = image.clone()
        val visited = Mat.zeros(image.size(), CvType.CV_8U)

        val sy = seed.y.toInt()
        val sx = seed.x.toInt()

        if (sy !in 0 until height || sx !in 0 until width) return resultMat

        val regionTint = randomColor()
        val queue: Queue<Point> = LinkedList()
        queue.add(seed)

        val initialSeedColor = image.getRGB(sx, sy)
        visited.put(sy, sx, 255.0)

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            val cy = current.y.toInt()
            val cx = current.x.toInt()

            val original = image.getRGB(cx, cy)
            onPixelAdded(original)
            resultMat.putRGB(cx, cy, regionTint)

            val neighbors = getNeighbors(cx, cy, width, height)
            for (neighbor in neighbors) {
                val ny = neighbor.y.toInt()
                val nx = neighbor.x.toInt()

                if (visited.get(ny, nx)[0] == 0.0) {
                    val neighborColor = image.getRGB(nx, ny)

                    if (shouldInclude(neighborColor, initialSeedColor)) {
                        visited.put(ny, nx, 255.0)
                        queue.add(neighbor)
                    }
                }
            }
        }

        visited.release()
        return resultMat
    }

    private fun getNeighbors(x: Int, y: Int, w: Int, h: Int): List<Point> {
        val dx = if (connectivity == 4) intArrayOf(0, 0, -1, 1) else intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)
        val dy = if (connectivity == 4) intArrayOf(-1, 1, 0, 0) else intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)

        return dx.indices.mapNotNull { i ->
            val nx = x + dx[i]
            val ny = y + dy[i]

            if (nx in 0 until w && ny in 0 until h)
                Point(nx.toDouble(), ny.toDouble())
            else
                null
        }
    }
}

class FixedRegionGrowing(seed: Point, threshold: Double, connectivity: Int)
    : RegionGrowing(seed, threshold, connectivity) {

    override fun shouldInclude(neighborColor: Color, seedColor: Color): Boolean {
        return neighborColor.euclidean(seedColor) <= threshold
    }
}

class FloatingRegionGrowing(seed: Point, threshold: Double, connectivity: Int)
    : RegionGrowing(seed, threshold, connectivity) {

    private var avgR = 0.0
    private var avgG = 0.0
    private var avgB = 0.0
    private var pixelCount = 0

    override fun onPixelAdded(pixelColor: Color) {
        pixelCount++
        avgR += (pixelColor.red - avgR) / pixelCount
        avgG += (pixelColor.green - avgG) / pixelCount
        avgB += (pixelColor.blue - avgB) / pixelCount
    }

    override fun shouldInclude(neighborColor: Color, seedColor: Color): Boolean {
        val currentAverage = Color(avgR.toInt(), avgG.toInt(), avgB.toInt())
        return neighborColor.euclidean(currentAverage) <= threshold
    }
}