package org.starcoin.sirius.serialization

import kotlinx.serialization.*

@Serializable
data class NamedData(@SerialId(1) val name: String, @SerialId(2) @Optional val data: TestData? = null) {

    @Serializer(forClass = NamedData::class)
    companion object : KSerializer<NamedData> {

    }
}
