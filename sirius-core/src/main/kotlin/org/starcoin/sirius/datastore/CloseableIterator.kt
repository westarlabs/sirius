package org.starcoin.sirius.datastore

import java.io.Closeable

class CloseableIterator<T>(iterator: Iterator<T>, private val closeAction: () -> Unit = {}) : Iterator<T> by iterator,
    Closeable {

    override fun close() {
        closeAction()
    }

    fun <R> map(transform: (T) -> R): CloseableIterator<R> {
        return CloseableIterator(this.iterator().asSequence().map(transform).iterator(), closeAction)
    }
}