package org.starcoin.sirius.core

open abstract class CachedHash : Hashable {

    val id: Hash by lazy { Hash.of(this.hashData()) }

    override fun hash(): Hash {
        return id
    }

    protected abstract fun hashData(): ByteArray


}
