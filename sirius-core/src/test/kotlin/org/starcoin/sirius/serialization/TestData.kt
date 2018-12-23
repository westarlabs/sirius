package org.starcoin.sirius.serialization

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.starcoin.sirius.core.SiriusObject

@Serializable
data class TestData(
    @SerialId(1)
    var booleanValue: Boolean,
    @SerialId(2)
    var intValue: Int,
    @SerialId(3)
    var stringValue: String,
    @SerialId(4)
    val bytesValue: ByteArrayWrapper
) : SiriusObject() {

    companion object {
        fun random(): TestData {
            return TestData(
                RandomUtils.nextBoolean(),
                RandomUtils.nextInt(),
                RandomStringUtils.randomAlphabetic(
                    RandomUtils.nextInt(
                        10,
                        30
                    )
                ),
                ByteArrayWrapper(
                    RandomUtils.nextBytes(
                        RandomUtils.nextInt(
                            10,
                            100
                        )
                    )
                )
            )
        }
    }

}
