package org.pdi.core.quantization

import org.opencv.core.*
import org.pdi.core.image.*
import java.awt.Color

private class Node(val level: Int) {
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var pixelCount = 0
    val children = arrayOfNulls<Node>(8)

    val isLeaf: Boolean get() = children.all { it == null }

    fun getIndex(r: Int, g: Int, b: Int): Int {
        val bit = 7 - level
        val rBit = (r shr bit) and 1
        val gBit = (g shr bit) and 1
        val bBit = (b shr bit) and 1
        return (rBit shl 2) or (gBit shl 1) or bBit
    }

    fun getAverageColor() = if (pixelCount == 0) Color.BLACK else Color(
        (rSum / pixelCount).toInt().coerceIn(0, 255),
        (gSum / pixelCount).toInt().coerceIn(0, 255),
        (bSum / pixelCount).toInt().coerceIn(0, 255)
    )
}

class OctreeQuantizer(k: Int) : Quantizer(k) {
    override fun apply(image: Image): Mat {
        val root = Node(0)
        val levels = Array(8) { mutableListOf<Node>() }
        var leafCount = 0

        image.readAllPixels { _, _, c ->
            var curr = root

            for (lvl in 0..7) {
                val idx = curr.getIndex(c.red, c.green, c.blue)

                if (curr.children[idx] == null) {
                    val newNode = Node(lvl + 1)
                    curr.children[idx] = newNode

                    if (lvl < 7) {
                        levels[lvl].add(newNode)
                    } else {
                        leafCount++
                    }
                }

                curr = curr.children[idx]!!
                if (curr.isLeaf) break
            }

            curr.rSum += c.red;
            curr.gSum += c.green;
            curr.bSum += c.blue
            curr.pixelCount++

            while (leafCount > value) {
                val depth = levels.indices.lastOrNull { levels[it].isNotEmpty() } ?: break
                val parent = levels[depth].removeAt(0)

                val childrenNodes = parent.children.filterNotNull()
                childrenNodes.forEach { child ->
                    parent.rSum += child.rSum
                    parent.gSum += child.gSum
                    parent.bSum += child.bSum
                    parent.pixelCount += child.pixelCount
                }

                leafCount -= (childrenNodes.size - 1)
                parent.children.fill(null)
            }
        }

        val res = Mat(image.image.size(), CvType.CV_8UC3)
        image.readAllPixels { x, y, c ->
            var node = root
            while (!node.isLeaf) {
                node = node.children[node.getIndex(c.red, c.green, c.blue)] ?: break
            }
            val avg = node.getAverageColor()
            res.put(y, x, avg.blue.toDouble(), avg.green.toDouble(), avg.red.toDouble())
        }

        return res
    }
}