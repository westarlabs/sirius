package org.starcoin.sirius.wallet.core.store

import com.alibaba.fastjson.JSON

class MemoryStore<T>(private val clazz: Class<T>) : Store<T> {

    private var bytes: ByteArray? = null

    fun save(t: T) {
        bytes = JSON.toJSONString(t).getBytes()
    }

    fun load(): T? {
        if (bytes != null) {
            println(String(bytes!!))
            return JSON.parseObject(bytes, clazz)
        }
        return null
    }
}