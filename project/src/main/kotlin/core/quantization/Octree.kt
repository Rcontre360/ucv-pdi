package org.pdi.core.quantization

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.pdi.core.image.Image
import java.awt.Color

class OctreeQuantizer(private val k: Int) : Quantizer(k) {

    private class Node(val level: Int) {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var pixelCount = 0
        val children = arrayOfNulls<Node>(8)

        val isLeaf: Boolean
            get() = pixelCount > 0 && children.all { it == null }

        fun getIndex(r: Int, g: Int, b: Int): Int {
            var index = 0
            val mask = 0x80 shr level
            if (r and mask != 0) index = index or 4
            if (g and mask != 0) index = index or 2
            if (b and mask != 0) index = index or 1
            return index
        }

        fun getAverageColor(): Color {
            return if (pixelCount == 0) Color(0, 0, 0)
            else Color((rSum / pixelCount).toInt(), (gSum / pixelCount).toInt(), (bSum / pixelCount).toInt())
        }
    }

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

            curr.rSum += c.red.toLong()
            curr.gSum += c.green.toLong()
            curr.bSum += c.blue.toLong()
            curr.pixelCount++

            while (leafCount > k) {
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
                val idx = node.getIndex(c.red, c.green, c.blue)
                node = node.children[idx] ?: break
            }
            val avg = node.getAverageColor()
            res.put(y, x, avg.blue.toDouble(), avg.green.toDouble(), avg.red.toDouble())
        }

        return res
    }
}