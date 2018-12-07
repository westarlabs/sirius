package org.starcoin.sirius.io

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.experimental.and

class ByteBufferInputStream(val byteBuffer: ByteBuffer) : InputStream() {

    constructor(bufferSize: Int) : this(ByteBuffer.allocate(bufferSize)) {}

    @Throws(IOException::class)
    override fun read(): Int {
        return if (!byteBuffer.hasRemaining()) {
            -1
        } else (byteBuffer.get() and 0xFF.toByte()).toInt()
    }

    @Throws(IOException::class)
    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) {
            return 0
        }
        val count = Math.min(byteBuffer.remaining(), length)
        if (count == 0) {
            return -1
        }
        byteBuffer.get(bytes, offset, count)
        return count
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return byteBuffer.remaining()
    }
}
