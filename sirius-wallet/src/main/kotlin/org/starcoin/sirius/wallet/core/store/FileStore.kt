package org.starcoin.sirius.wallet.core.store

import com.alibaba.fastjson.JSON
import com.google.common.base.Preconditions
import java.io.File
import java.nio.file.Path
import org.starcoin.sirius.util.FileUtil

class FileStore<T>(path: String, private val clazz: Class<T>) : Store<T> {

    private val path: Path

    init {
        val file = File(path)
        Preconditions.checkState(file.exists())
        Preconditions.checkState(file.isDirectory)
        this.path = file.toPath()
    }

    override fun save(t: T) {
        val file = File(path.toString() + File.separator + "hub.data")
        if (file.exists()) {
            file.delete()
        }
        FileUtil.writeFile(file, JSON.toJSONString(t).toByteArray())
    }

    override fun load(): T? {
        val file = File(path.toString() + File.separator + "hub.data")
        if (!file.exists()) {
            return null
        }
        val content = FileUtil.readFile(file)
        return JSON.parseObject<T>(content, clazz)
    }
}
