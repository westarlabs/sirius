package org.starcoin.sirius.io

import com.google.common.io.ByteStreams

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

object IOUtils {

    @Throws(IOException::class)
    fun toByteBuffer(`in`: InputStream): ByteBuffer {
        return ByteBuffer.wrap(ByteStreams.toByteArray(`in`))
    }

    fun close(closeable: Closeable?) {
        if (closeable == null) {
            return
        }
        try {
            closeable.close()
        } catch (e: IOException) {
            // just ignore
        }

    }
}
