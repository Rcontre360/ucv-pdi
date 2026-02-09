package org.pdi.core.rg

import org.opencv.core.*
import java.util.*

abstract class RegionGrowing(val threshold: Double, val connectivity: Int) {

    abstract fun shouldInclude(neighborColor: DoubleArray, seedColor: DoubleArray, lastAddedColor: DoubleArray): Boolean

    fun execute(image: Mat, seeds: List<Point>): Mat {
        val height = image.rows()
        val width = image.cols()
        val resultMat = image.clone()
        val visited = Mat.zeros(image.size(), CvType.CV_8U)

        // Colores aleatorios para cada región
        val random = Random()

        for (seed in seeds) {
            if (visited.get(seed.y.toInt(), seed.x.toInt())[0] != 0.0) continue

            val regionColor = doubleArrayOf(random.nextDouble() * 255, random.nextDouble() * 255, random.nextDouble() * 255)
            val queue: Queue<Point> = LinkedList()
            queue.add(seed)

            val seedColor = image.get(seed.y.toInt(), seed.x.toInt())
            var lastColor = seedColor

            visited.put(seed.y.toInt(), seed.x.toInt(), 255.0)

            while (queue.isNotEmpty()) {
                val current = queue.poll()
                val cy = current.y.toInt()
                val cx = current.x.toInt()

                // Pintar el píxel en el resultado (tintado simple)
                val original = image.get(cy, cx)
                resultMat.put(cy, cx,
                    (original[0] * 0.5 + regionColor[0] * 0.5),
                    (original[1] * 0.5 + regionColor[1] * 0.5),
                    (original[2] * 0.5 + regionColor[2] * 0.5)
                )

                // Obtener vecinos según conectividad (4 u 8)
                val neighbors = getNeighbors(cx, cy, width, height)
                for (neighbor in neighbors) {
                    val ny = neighbor.y.toInt()
                    val nx = neighbor.x.toInt()

                    if (visited.get(ny, nx)[0] == 0.0) {
                        val neighborColor = image.get(ny, nx)

                        if (shouldInclude(neighborColor, seedColor, lastColor)) {
                            visited.put(ny, nx, 255.0)
                            queue.add(neighbor)
                            lastColor = neighborColor // Actualizar para rango flotante
                        }
                    }
                }
            }
        }
        visited.release()
        return resultMat
    }

    private fun getNeighbors(x: Int, y: Int, w: Int, h: Int): List<Point> {
        val points = mutableListOf<Point>()
        val range = if (connectivity == 8) -1..1 else 0..0 // Simplificación lógica

        // Vecindad 4: Arriba, Abajo, Izquierda, Derecha
        if (connectivity == 4) {
            if (y > 0) points.add(Point(x.toDouble(), (y - 1).toDouble()))
            if (y < h - 1) points.add(Point(x.toDouble(), (y + 1).toDouble()))
            if (x > 0) points.add(Point((x - 1).toDouble(), y.toDouble()))
            if (x < w - 1) points.add(Point((x + 1).toDouble(), y.toDouble()))
        } else {
            // Vecindad 8
            for (i in -1..1) {
                for (j in -1..1) {
                    if (i == 0 && j == 0) continue
                    val nx = x + i
                    val ny = y + j
                    if (nx in 0 until w && ny in 0 until h) {
                        points.add(Point(nx.toDouble(), ny.toDouble()))
                    }
                }
            }
        }
        return points
    }

    protected fun euclideanDistance(c1: DoubleArray, c2: DoubleArray): Double {
        val db = c1[0] - c2[0]
        val dg = c1[1] - c2[1]
        val dr = c1[2] - c2[2]
        return Math.sqrt(db * db + dg * dg + dr * dr)
    }
}

class FixedRegionGrowing(threshold: Double, connectivity: Int) : RegionGrowing(threshold, connectivity) {
    override fun shouldInclude(neighborColor: DoubleArray, seedColor: DoubleArray, lastAddedColor: DoubleArray): Boolean {
        // La distancia es siempre respecto a la semilla
        return euclideanDistance(neighborColor, seedColor) <= threshold
    }
}

class FloatingRegionGrowing(threshold: Double, connectivity: Int) : RegionGrowing(threshold, connectivity) {
    override fun shouldInclude(neighborColor: DoubleArray, seedColor: DoubleArray, lastAddedColor: DoubleArray): Boolean {
        // La distancia es respecto al píxel anterior (crecimiento dinámico)
        return euclideanDistance(neighborColor, lastAddedColor) <= threshold
    }
}