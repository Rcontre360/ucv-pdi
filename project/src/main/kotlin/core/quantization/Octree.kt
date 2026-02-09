package org.pdi.core.quantization

import org.opencv.core.Mat
import org.pdi.core.image.Image
import org.pdi.core.image.putRGB
import java.awt.Color

private class Node(val level: Int, var isLeaf: Boolean = false) {
    var rSum = 0L; var gSum = 0L; var bSum = 0L
    var pixelCount = 0
    val children = arrayOfNulls<Node>(8)

    fun getIndex(r: Int, g: Int, b: Int): Int {
        val shift = 7 - level
        return ((r shr shift and 1) shl 2) or ((g shr shift and 1) shl 1) or (b shr shift and 1)
    }

    fun getAverageColor(): Color {
        return if (pixelCount == 0) Color.BLACK
        else Color(rSum.toInt() / pixelCount, gSum.toInt() / pixelCount, bSum.toInt() / pixelCount)
    }
}

class OctreeQuantizer(k: Int) : Quantizer(k) {
    override fun apply(image: Image): Mat {
        val root = Node(0)
        val levels = Array(8) { mutableListOf<Node>() }
        var leafCount = 0

        image.readAllPixels { _, _, color ->
            var current = root
            for (lvl in 0..7) {
                val idx = current.getIndex(color.red, color.green, color.blue)
                if (current.children[idx] == null) {
                    current.children[idx] = Node(lvl + 1, lvl == 7)
                    if (lvl < 7) levels[lvl].add(current.children[idx]!!) else leafCount++
                }
                current = current.children[idx]!!
                if (current.isLeaf) break
            }
            current.rSum += color.red; current.gSum += color.green; current.bSum += color.blue
            current.pixelCount++

            while (leafCount > value) {
                val deepestLevel = levels.indices.last { levels[it].isNotEmpty() }
                val nodeToReduce = levels[deepestLevel].removeAt(0)

                var childrenCount = 0
                nodeToReduce.children.filterNotNull().forEach { child ->
                    nodeToReduce.rSum += child.rSum
                    nodeToReduce.gSum += child.gSum
                    nodeToReduce.bSum += child.bSum
                    nodeToReduce.pixelCount += child.pixelCount
                    childrenCount++
                }
                nodeToReduce.isLeaf = true
                leafCount -= (childrenCount - 1)
            }
        }

        val result = Mat.zeros(image.image.size(), image.image.type())
        image.readAllPixels { x, y, color ->
            var node = root
            while (!node.isLeaf) {
                node = node.children[node.getIndex(color.red, color.green, color.blue)]!!
            }
            result.putRGB(x, y, node.getAverageColor())
        }
        return result
    }
}