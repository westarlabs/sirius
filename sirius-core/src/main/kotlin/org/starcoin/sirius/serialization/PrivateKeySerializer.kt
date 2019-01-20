package org.starcoin.sirius.serialization

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import java.security.PrivateKey

@Serializer(forClass = PrivateKey::class)
class PrivateKeySerializer : KSerializer<PrivateKey> {

    override fun deserialize(input: Decoder): PrivateKey {
        return when (input) {
            is BinaryDecoder -> CryptoService.loadPrivateKey(
                input.decodeByteArray()
            )
            else -> CryptoService.loadPrivateKey(
                input.decodeString().hexToByteArray()
            )
        }
    }

    override fun serialize(output: Encoder, obj: PrivateKey) {
        when (output) {
            is BinaryEncoder -> output.encodeByteArray(
                CryptoService.encodePrivateKey(
                    obj
                )
            )
            else -> output.encodeString(
                CryptoService.encodePrivateKey(
                    obj
                ).toHEXString()
            )
        }
    }
}
