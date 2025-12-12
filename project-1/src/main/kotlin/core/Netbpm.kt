package org.pdi.io

import org.pdi.core.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.PrintWriter
import java.awt.Color
import java.awt.Graphics2D
import javax.imageio.ImageIO

fun saveImage(dir: String, format: String, image: Image){
    val output = File(dir)
    val targetType = when (Pair(image.isGrayscale,image.isBinary)) {
        Pair(true,true) -> BufferedImage.TYPE_BYTE_BINARY
        Pair(true,false) -> BufferedImage.TYPE_BYTE_GRAY
        else -> BufferedImage.TYPE_INT_RGB
    }

    val newImage = BufferedImage(
        image.metadata.width,
        image.metadata.height,
        targetType
    )

    val g2d: Graphics2D = newImage.createGraphics()
    g2d.drawImage(image.image, 0, 0, null)

    when (format){
        "png" -> ImageIO.write(newImage, format, output)
        "bmp" -> ImageIO.write(newImage, format, output)
        "netpbm" -> NetpbmWriter.write(newImage, targetType, dir)
        else -> throw IllegalArgumentException("$format is not a supported file type")
    }
}

object NetpbmWriter {
    fun write(image: BufferedImage, type: Int, filePath: String) {
        val file = File(filePath)

        PrintWriter(file.bufferedWriter()).use { writer ->
            when (type) {
                BufferedImage.TYPE_BYTE_BINARY -> writePbm(image, writer)
                BufferedImage.TYPE_BYTE_GRAY -> writePgm(image, writer)
                BufferedImage.TYPE_INT_RGB -> writePpm(image, writer)
            }
        }
    }

    private fun writePbm(image: BufferedImage, writer: PrintWriter) {
        writer.println("P1")
        writer.println("${image.width} ${image.height}")

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val grayValue = Color(image.getRGB(x, y)).red
                val pbmValue = if (grayValue > 127) 0 else 1 // Convert to 0 or 1
                writer.print("$pbmValue ")
            }
            writer.println()
        }
    }

    private fun writePgm(image: BufferedImage, writer: PrintWriter) {
        writer.println("P2")
        writer.println("${image.width} ${image.height}")
        writer.println("255")

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val grayValue = Color(image.getRGB(x, y)).red
                writer.print("$grayValue ")
            }
            writer.println()
        }
    }

    private fun writePpm(image: BufferedImage, writer: PrintWriter) {
        writer.println("P3")
        writer.println("${image.width} ${image.height}")
        writer.println("255")

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val color = Color(image.getRGB(x, y))
                writer.print("${color.red} ${color.green} ${color.blue} ")
            }
            writer.println()
        }
    }
}