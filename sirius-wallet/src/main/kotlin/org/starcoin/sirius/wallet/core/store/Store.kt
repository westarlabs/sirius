package org.starcoin.sirius.wallet.core.store

interface Store<T> {

    fun save(t: T)

    fun load(): T?
}