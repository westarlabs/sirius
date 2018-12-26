package org.starcoin.sirius.core

import kotlinx.serialization.Transient
import org.starcoin.sirius.lang.resetableLazy

open abstract class CachedHashable : Hashable {

    @Transient
    private val hashDelegate = resetableLazy { doHash() }

    @Transient
    val id: Hash by hashDelegate

    override fun hash(): Hash {
        return id
    }

    protected open fun doHash(): Hash {
        return Hash.of(this.hashData())
    }

    protected open fun hashData(): ByteArray {
        TODO()
    }

    protected fun resetHash() {
        this.hashDelegate.reset()
    }

}
