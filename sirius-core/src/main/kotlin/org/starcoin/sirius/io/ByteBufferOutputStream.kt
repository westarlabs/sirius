package org.starcoin.sirius.io

import java.io.OutputStream
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

/**
 * Wraps a [ByteBuffer] so it can be used like an [OutputStream]. This is similar to a
 * [java.io.ByteArrayOutputStream], just that this uses a `ByteBuffer` instead of a
 * `byte[]` as internal storage.
 */
class ByteBufferOutputStream @JvmOverloads constructor(
    private var wrappedBuffer: ByteBuffer?,
    private val autoEnlarge: Boolean = true
) : OutputStream() {

    @JvmOverloads
    constructor(size: Int = 32) : this(ByteBuffer.allocate(size), true) {
    }

    fun toByteBuffer(): ByteBuffer {

        val byteBuffer = wrappedBuffer!!.duplicate()
        byteBuffer.flip()
        return byteBuffer.asReadOnlyBuffer()
    }

    /**
     * Resets the `count` field of this byte array output stream to zero, so that all
     * currently accumulated output in the output stream is discarded. The output stream can be used
     * again, reusing the already allocated buffer space.
     *
     * @see java.io.ByteArrayInputStream.count
     */
    fun reset() {
        wrappedBuffer!!.rewind()
    }

    /**
     * Increases the capacity to ensure that it can hold at least the number of elements specified by
     * the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private fun growTo(minCapacity: Int) {

        // overflow-conscious code
        val oldCapacity = wrappedBuffer!!.capacity()
        var newCapacity = oldCapacity shl 1
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity
        }
        if (newCapacity < 0) {
            if (minCapacity < 0) { // overflow
                throw OutOfMemoryError()
            }
            newCapacity = Integer.MAX_VALUE
        }
        val oldWrappedBuffer = wrappedBuffer
        // create the new buffer
        if (wrappedBuffer!!.isDirect) {
            wrappedBuffer = ByteBuffer.allocateDirect(newCapacity)
        } else {
            wrappedBuffer = ByteBuffer.allocate(newCapacity)
        }
        // copy over the old content into the new buffer
        oldWrappedBuffer!!.flip()
        wrappedBuffer!!.put(oldWrappedBuffer)
    }

    override fun write(bty: Int) {

        try {
            wrappedBuffer!!.put(bty.toByte())
        } catch (ex: BufferOverflowException) {
            if (autoEnlarge) {
                val newBufferSize = wrappedBuffer!!.capacity() * 2
                growTo(newBufferSize)
                write(bty)
            } else {
                throw ex
            }
        }

    }

    override fun write(bytes: ByteArray) {

        var oldPosition = 0
        try {
            oldPosition = wrappedBuffer!!.position()
            wrappedBuffer!!.put(bytes)
        } catch (ex: BufferOverflowException) {
            if (autoEnlarge) {
                val newBufferSize = Math.max(wrappedBuffer!!.capacity() * 2, oldPosition + bytes.size)
                growTo(newBufferSize)
                write(bytes)
            } else {
                throw ex
            }
        }

    }

    override fun write(bytes: ByteArray, off: Int, len: Int) {

        var oldPosition = 0
        try {
            oldPosition = wrappedBuffer!!.position()
            wrappedBuffer!!.put(bytes, off, len)
        } catch (ex: BufferOverflowException) {
            if (autoEnlarge) {
                val newBufferSize = Math.max(wrappedBuffer!!.capacity() * 2, oldPosition + len)
                growTo(newBufferSize)
                write(bytes, off, len)
            } else {
                throw ex
            }
        }

    }
}
