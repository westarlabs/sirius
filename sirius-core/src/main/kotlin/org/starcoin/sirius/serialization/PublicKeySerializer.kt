package org.starcoin.sirius.serialization

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import java.security.PublicKey

@Serializer(forClass = PublicKey::class)
class PublicKeySerializer : KSerializer<PublicKey> {

    override fun deserialize(input: Decoder): PublicKey {
        return when (input) {
            is BinaryDecoder -> CryptoService.loadPublicKey(
                input.decodeByteArray()
            )
            else -> CryptoService.loadPublicKey(
                input.decodeString().hexToByteArray()
            )
        }
    }

    override fun serialize(output: Encoder, obj: PublicKey) {
        when (output) {
            is BinaryEncoder -> output.encodeByteArray(
                CryptoService.encodePublicKey(
                    obj
                )
            )
            else -> output.encodeString(
                CryptoService.encodePublicKey(
                    obj
                ).toHEXString()
            )
        }
    }
}
