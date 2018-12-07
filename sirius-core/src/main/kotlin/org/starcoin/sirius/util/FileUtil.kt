package org.starcoin.sirius.util

import java.io.*
import java.nio.file.Files
import java.util.*

object FileUtil {

    fun writeFile(filename: String, data: ByteArray) {
        writeFile(File(filename), data)
    }

    fun writeFile(file: File, data: ByteArray) {
        try {
            val out = FileOutputStream(file)
            out.write(data)
            out.close()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    fun readFile(filename: String): ByteArray {
        return readFile(File(filename))
    }

    fun readFile(file: File): ByteArray {
        try {
            val len = file.length().toInt()
            val data = ByteArray(len)
            val `in` = DataInputStream(FileInputStream(file))
            `in`.read(data)
            `in`.close()

            return data
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    fun deleteDir(dir: File) {
        try {
            Files.walk(dir.toPath())
                .map { it.toFile() }
                .sorted(Comparator.reverseOrder())
                .forEach { it.delete() }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }
}
