package org.pdi.io

// this file system module is used to save netbpm and compressed files

import org.opencv.core.Mat
import org.opencv.core.CvType
import org.pdi.core.Image
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.Locale
import java.util.Scanner
import javax.imageio.ImageIO

// this save image is the entry point of this module
fun saveImage(dir: String, format: String, image: Image) {
    val output = File(dir)
    // we check the target type using the image fields
    val targetType = when (Pair(image.isGrayscale, image.isBinary)) {
        Pair(true, true) -> BufferedImage.TYPE_BYTE_BINARY
        Pair(true, false) -> BufferedImage.TYPE_BYTE_GRAY
        else -> BufferedImage.TYPE_INT_RGB
    }

    // the format is given by the UI
    when (format) {
        // we have 2 customs ways to save. PDI (compressed stuff) and Netpbm
        "rle" -> PdiIO.write(image, targetType, output.absolutePath)
        "netpbm" -> NetpbmIO.write(image, targetType, output.absolutePath)
        else -> ImageIO.write(image.buff, format, output)
    }
}

// entry point to load an image
fun loadImage(file: File): Image {
    val extension = file.extension.lowercase(Locale.getDefault())
    val bufferedImage = when {
        // usually we should check the HEADER to ensure the file is correct
        extension in listOf("pbm", "pgm", "ppm", "netpbm") -> NetpbmIO.read(file).buff
        extension == "rle" -> PdiIO.read(file).buff
        else -> ImageIO.read(file) ?: throw IOException("Could not read image file: ${file.absolutePath}")
    }
    return Image(bufferedImage.toMat())
}

// utility to store as Netpbm
object NetpbmIO {
    fun read(file: File): Image {
        val fileScan = Scanner(file)
        return read(fileScan)
    }

    // reads given a scanner. Done like this because we need this class for the compressionn
    fun read(scanner: Scanner): Image {
        val imgType = scanner.next()
        val width = scanner.nextInt()
        val height = scanner.nextInt()

        val mat = Mat(height, width, CvType.CV_8UC3)

        // checking the image type. If its not P1 we must read the next int
        if (imgType == "P1") 1 else scanner.nextInt()

        for (y in 0 until height) {
            for (x in 0 until width) {
                // given the image type we read and load differently
                when (imgType) {
                    "P1" -> {
                        // single color binary
                        val pbmValue = scanner.nextInt()
                        val colorValue = if (pbmValue == 1) 0.0 else 255.0
                        mat.put(y, x, colorValue, colorValue, colorValue)
                    }
                    "P2" -> {
                        // single color
                        val gray = scanner.nextInt()
                        mat.put(y, x, gray.toDouble(), gray.toDouble(), gray.toDouble())
                    }
                    "P3" -> {
                        // we load 3 colors
                        val r = scanner.nextInt().coerceIn(0, 255)
                        val g = scanner.nextInt().coerceIn(0, 255)
                        val b = scanner.nextInt().coerceIn(0, 255)
                        mat.put(y, x, b.toDouble(), g.toDouble(), r.toDouble())
                    }
                }
            }
        }

        return Image(mat)
    }

    // we write the image
    fun write(image: Image, type: Int, filePath: String) {
        val file = File(filePath)
        PrintWriter(file.bufferedWriter()).use { writer ->
            // we just create a long string of the file
            writer.print(getString(image, type))
        }
    }

    // pixel list in the shape of a list because its easier to manage on compression if used
    fun getPixelList(image: Image, type: Int): List<String> {
        return when (type) {
            BufferedImage.TYPE_BYTE_BINARY -> pbmPixelList(image)
            BufferedImage.TYPE_BYTE_GRAY -> pgmPixelList(image)
            else -> ppmPixelList(image)
        }
    }

    // uses get pixel list and mixes them with the header and the separators
    fun getString(image: Image, type: Int): String {
        val header = when (type) {
            BufferedImage.TYPE_BYTE_BINARY -> "P1\n${image.metadata.width} ${image.metadata.height}\n"
            BufferedImage.TYPE_BYTE_GRAY -> "P2\n${image.metadata.width} ${image.metadata.height}\n255\n"
            else -> "P3\n${image.metadata.width} ${image.metadata.height}\n255\n"
        }
        return header + getPixelList(image, type).joinToString(separator = " ")
    }

    // binary one
    private fun pbmPixelList(image: Image): List<String> {
        val pixels = mutableListOf<String>()
        image.readAllPixels { x, _, color ->
            val grayValue = color[2].toInt() // Assuming BGR, R is at index 2
            val pbmValue = if (grayValue > 127) 0 else 1
            pixels.add(pbmValue.toString())
            if (x == image.metadata.width-1)
                pixels.add("\n")
        }
        return pixels
    }

    // gray pixel list
    private fun pgmPixelList(image: Image): List<String> {
        val pixels = mutableListOf<String>()
        image.readAllPixels { x, _, color ->
            val grayValue = color[2].toInt() // Assuming BGR, R is at index 2
            pixels.add(grayValue.toString())
            if (x == image.metadata.width-1)
                pixels.add("\n")
        }
        return pixels
    }

    // color pixel list
    private fun ppmPixelList(image: Image): List<String> {
        val pixels = mutableListOf<String>()
        image.readAllPixels { x, _, color ->
            pixels.add("${color[2].toInt()} ${color[1].toInt()} ${color[0].toInt()}") // BGR to RGB
            if (x == image.metadata.width-1)
                pixels.add("\n")
        }
        return pixels
    }
}

// compressed logic
object PdiIO {
    fun read(file: File): Image {
        val scanner = Scanner(file)
        val filetype = scanner.nextLine()

        // I check the header
        if (!filetype.matches("PDI[1-3]".toRegex())) throw IllegalArgumentException("Not a PDI file")

        // YES, I added a checksum! just for fun
        val checksum = scanner.nextLine().toInt()
        val dims = scanner.nextLine().split(" ")
        val width = dims[0].toInt()
        val height = dims[1].toInt()
        // decompressing the pixels
        val netpbmPixels = decompress(scanner.useDelimiter("\\A").next())
        // different headers for different types of storage. The same as with netbpm. But with PDI
        val header = when (filetype) {
            "PDI3" -> "P3\n$width $height\n255\n"
            "PDI1" -> "P1\n$width $height\n"
            "PDI2" -> "P2\n$width $height\n255\n"
            else -> throw IllegalArgumentException("Unknown PDI type: $filetype")
        }

        // we load the content, put it in a scanner
        val fullContent = header + netpbmPixels.joinToString(" ")
        val strScan = Scanner(fullContent)
        if (checksum(fullContent) != checksum) {
            println("checksum wrong")
        }

        // this is why we left the read open with a scanner argument
        return NetpbmIO.read(strScan)
    }

    // decompress function
    private fun decompress(data: String): List<String> {
        val decompressed = mutableListOf<String>()
        val pairs = data.split("|").filter { it.isNotBlank() }
        for (pair in pairs) {
            // we only have a "," delimiter between values.
            val pair = pair.split(",")
            // creating a netbpm format out of the compressed format
            repeat(pair[1].toInt()) {
                decompressed.add(pair[0])
            }
        }
        return decompressed
    }

    // write the compressed file
    fun write(image: Image, type: Int, filePath: String) {
        // we first transform it to Netpbm format, as a string
        val content = NetpbmIO.getString(image, type)
        // calculate its checksum to verify the file integrity (and that we did everything correctly)
        val checksum = checksum(content)
        // we compress the pixel list
        val compressedContent = compress(NetpbmIO.getPixelList(image, type))

        PrintWriter(File(filePath).bufferedWriter()).use { writer ->
            val formatType = when(type){
                BufferedImage.TYPE_BYTE_BINARY -> "PDI1"
                BufferedImage.TYPE_BYTE_GRAY -> "PDI2"
                else -> "PDI3"
            }
            // our file format is "HEADER, CHECKSUM, (WIDTH,HEIGHT), CONTENT"
            writer.println(formatType)
            writer.println(checksum)
            writer.println("${image.metadata.width} ${image.metadata.height}")
            writer.print(compressedContent)
        }
    }

    // compress function
    private fun compress(data: List<String>): String {
        if (data.isEmpty()) return ""
        val res = mutableListOf<String>()
        var currentP = data[0]
        var cnt = 0

        // we count how many colors we have
        for (p in data) {
            if (p == currentP) {
                cnt++
            } else {
                res.add("$currentP,$cnt")
                currentP = p
                cnt = 1
            }
        }
        // and store the compressed colors
        res.add("$currentP,$cnt")

        return res.joinToString(separator = "|")
    }

    // checksum function can be changed
    fun checksum(data: String): Int {
        return data.hashCode()
    }
}