package org.starcoin.sirius.util

import java.io.File
import java.nio.file.Files
import java.util.*

@Deprecated("Directly use File extension function")
object FileUtil {

    fun writeFile(filename: String, data: ByteArray) {
        writeFile(File(filename), data)
    }

    fun writeFile(file: File, data: ByteArray) {
        file.writeBytes(data)
    }

    fun readFile(filename: String): ByteArray {
        return readFile(File(filename))
    }

    fun readFile(file: File): ByteArray {
        return file.readBytes()
    }

    fun deleteDir(dir: File) {
        Files.walk(dir.toPath())
            .map { it.toFile() }
            .sorted(Comparator.reverseOrder())
            .forEach { it.delete() }
    }
}

fun File.readBytes(): ByteArray = this.inputStream().use {
    it.readBytes()
}

fun File.writeBytes(bytes: ByteArray) = this.outputStream().use { it.write(bytes) }