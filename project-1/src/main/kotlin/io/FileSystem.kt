package org.pdi.io

import org.pdi.core.Image
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.Locale
import java.util.Scanner
import javax.imageio.ImageIO

fun saveImage(dir: String, format: String, image: Image) {
    val output = File(dir)
    val targetType = when (Pair(image.isGrayscale, image.isBinary)) {
        Pair(true, true) -> BufferedImage.TYPE_BYTE_BINARY
        Pair(true, false) -> BufferedImage.TYPE_BYTE_GRAY
        else -> BufferedImage.TYPE_INT_RGB
    }

    val newImage = BufferedImage(
        image.metadata.width,
        image.metadata.height,
        targetType
    )

    val g2d: Graphics2D = newImage.createGraphics()
    g2d.drawImage(image.image, 0, 0, null)

    when (format) {
        "pdi" -> PdiWriter.write(newImage, targetType, output.absolutePath)
        "netpbm" -> NetpbmCodec.write(newImage, targetType, output.absolutePath)
        else -> ImageIO.write(newImage, format, output)
    }
}

fun loadImage(file: File): BufferedImage {
    val extension = file.extension.lowercase(Locale.getDefault())
    return when {
        extension in listOf("pbm", "pgm", "ppm", "netpbm") -> NetpbmCodec.read(file)
        extension == "pdi" -> PdiReader.read(file)
        else -> ImageIO.read(file) ?: throw IOException("Could not read image file: ${file.absolutePath}")
    }
}

object PdiReader {
    fun read(file: File): BufferedImage {
        val scanner = Scanner(file)
        val magicNumber = scanner.nextLine()
        if (magicNumber != "PDI") throw IllegalArgumentException("Not a PDI file")

        val expectedChecksum = scanner.nextLine().toInt()
        val dimensions = scanner.nextLine().split(" ")
        val width = dimensions[0].toInt()
        val height = dimensions[1].toInt()
        val compressedData = scanner.useDelimiter("\\A").next()
        val decompressedPixels = decompress(compressedData)

        val type = when {
            decompressedPixels.any { it.contains(" ") } -> {
                "P3"
            }
            decompressedPixels.any { it == "0" || it == "1"} -> {
                "P1"
            }
            else -> {
                "P2"
            }
        }

        val (header,image) = when(type) {
            "P3" -> {
                "P3\n$width $height\n255\n" to BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            }
           "P1" -> {
                println("BIN")
                "P1\n$width $height\n255\n" to BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY)
            }
            else -> {
                "P2\n$width $height\n" to BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
            }
        }

        val reconstructedContent = header + decompressedPixels.joinToString(" ")
        val actualChecksum = PdiWriter.checksum(reconstructedContent)
        val strScan = Scanner(reconstructedContent)

        if (actualChecksum != expectedChecksum) {
            println("checksum wrong")
        }

        return NetpbmCodec.read(strScan)
    }

    private fun decompress(data: String): List<String> {
        val decompressed = mutableListOf<String>()
        val pairs = data.split("|").filter { it.isNotBlank() }
        for (pair in pairs) {
            val pair = pair.split(",")
            repeat(pair[1].toInt()) {
                decompressed.add(pair[0])
            }
        }
        return decompressed
    }
}

object PdiWriter {
    fun write(image: BufferedImage, type: Int, filePath: String) {
        val content = NetpbmCodec.getString(image, type)
        val checksum = checksum(content)
        val compressedContent = compress(NetpbmCodec.getPixelList(image, type))

        PrintWriter(File(filePath).bufferedWriter()).use { writer ->
            writer.println("PDI")
            writer.println(checksum)
            writer.println("${image.width} ${image.height}")
            writer.print(compressedContent)
        }
    }

    private fun compress(data: List<String>): String {
        if (data.isEmpty()) return ""
        val res = mutableListOf<String>()
        var currentP = data[0]
        var cnt = 0

        for (p in data) {
            if (p == currentP) {
                cnt++
            } else {
                res.add("$currentP,$cnt")
                currentP = p
                cnt = 1
            }
        }
        res.add("$currentP,$cnt")

        return res.joinToString(separator = "|")
    }

    fun checksum(data: String): Int {
        return data.hashCode()
    }
}

object NetpbmCodec {
    fun read(file: File): BufferedImage {
        val fileScan = Scanner(file)
        return read(fileScan)
    }

    fun read(scanner: Scanner): BufferedImage {
        val magicNumber = scanner.next()
        while (scanner.hasNext() && scanner.hasNextInt().not()) {
            scanner.nextLine()
        }

        val width = scanner.nextInt()
        val height = scanner.nextInt()
        val maxVal = if (magicNumber == "P1") 1 else scanner.nextInt()

        val imageType = when (magicNumber) {
            "P3" -> BufferedImage.TYPE_INT_RGB
            "P2" -> BufferedImage.TYPE_BYTE_GRAY
            "P1" -> BufferedImage.TYPE_BYTE_BINARY
            else -> throw IllegalArgumentException("Unsupported Netpbm format: $magicNumber")
        }

        val image = BufferedImage(width, height, imageType)

        // Pixel Reading Loop
        when (magicNumber) {
            "P1" -> { // PBM ASCII (1-bit Binary)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        // We must convert 1/0 to Black/White RGB values
                        val pbmValue = scanner.nextInt()
                        // Netpbm convention: 1 is Black (Foreground), 0 is White (Background)
                        val color = if (pbmValue == 1) Color.BLACK.rgb else Color.WHITE.rgb
                        image.setRGB(x, y, color)
                    }
                }
            }
            "P2" -> { // PGM ASCII (Grayscale)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val gray = scanner.nextInt()
                        // Ensure bounds checking for color instantiation
                        val safeGray = gray.coerceIn(0, 255)
                        val color = Color(safeGray, safeGray, safeGray).rgb
                        image.setRGB(x, y, color)
                    }
                }
            }
            "P3" -> { // PPM ASCII (Color)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val r = scanner.nextInt().coerceIn(0, 255)
                        val g = scanner.nextInt().coerceIn(0, 255)
                        val b = scanner.nextInt().coerceIn(0, 255)
                        val color = Color(r, g, b).rgb
                        image.setRGB(x, y, color)
                    }
                }
            }
            // The else case is handled above
        }
        return image
    }

    fun getPixelList(image: BufferedImage, type: Int): List<String> {
        return when (type) {
            BufferedImage.TYPE_BYTE_BINARY -> pbmPixelList(image)
            BufferedImage.TYPE_BYTE_GRAY -> pgmPixelList(image)
            BufferedImage.TYPE_INT_RGB -> ppmPixelList(image)
            else -> emptyList()
        }
    }

    fun getString(image: BufferedImage, type: Int): String {
        val header = when (type) {
            BufferedImage.TYPE_BYTE_BINARY -> "P1\n${image.width} ${image.height}\n"
            BufferedImage.TYPE_BYTE_GRAY -> "P2\n${image.width} ${image.height}\n255\n"
            BufferedImage.TYPE_INT_RGB -> "P3\n${image.width} ${image.height}\n255\n"
            else -> ""
        }
        return header + getPixelList(image, type).joinToString(separator = " ")
    }

    fun write(image: BufferedImage, type: Int, filePath: String) {
        val file = File(filePath)
        PrintWriter(file.bufferedWriter()).use { writer ->
            writer.print(getString(image, type))
        }
    }

    private fun pbmPixelList(image: BufferedImage): List<String> {
        val pixels = mutableListOf<String>()
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val grayValue = Color(image.getRGB(x, y)).red
                val pbmValue = if (grayValue > 127) 0 else 1
                pixels.add(pbmValue.toString())
            }
            pixels.add("\n")
        }
        return pixels
    }

    private fun pgmPixelList(image: BufferedImage): List<String> {
        val pixels = mutableListOf<String>()
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val grayValue = Color(image.getRGB(x, y)).red
                pixels.add(grayValue.toString())
            }
            pixels.add("\n")
        }
        return pixels
    }

    private fun ppmPixelList(image: BufferedImage): List<String> {
        val pixels = mutableListOf<String>()
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val color = Color(image.getRGB(x, y))
                pixels.add("${color.red} ${color.green} ${color.blue}")
            }
            pixels.add("\n")
        }
        return pixels
    }
}
