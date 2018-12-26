package org.starcoin.sirius.serialization

import kotlinx.serialization.*
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.core.SiriusObjectCompanion

enum class TestEnum {
    TEST_ENUM_ZERO,
    TEST_ENUM_ONE
}

@Serializable
@ProtobufSchema(Starcoin.TestData::class)
data class TestData(
    @SerialId(1)
    var booleanValue: Boolean,
    @SerialId(2)
    var intValue: Int,
    @SerialId(3)
    var enumValue: TestEnum,
    @SerialId(4)
    var stringValue: String,
    @SerialId(5)
    var bytesValue: ByteArrayWrapper,
    @SerialId(6)
    @Optional
    var optionalValue: String = ""
) : SiriusObject() {

    @Serializer(forClass = TestData::class)
    companion object : SiriusObjectCompanion<TestData, Starcoin.TestData>(TestData::class), KSerializer<TestData> {

        override fun mock(): TestData {
            return random()
        }

        fun random(): TestData {
            return TestData(
                RandomUtils.nextBoolean(),
                RandomUtils.nextInt(),
                TestEnum.values()[RandomUtils.nextInt(0, TestEnum.values().size)],
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
                ),
                when (RandomUtils.nextBoolean()) {
                    true -> ""
                    false -> "not empty"
                }
            )
        }
    }

}
