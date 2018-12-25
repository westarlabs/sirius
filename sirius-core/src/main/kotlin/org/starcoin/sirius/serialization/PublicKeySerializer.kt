package org.starcoin.sirius.serialization

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.util.Utils
import java.security.PublicKey

@Serializer(forClass = PublicKey::class)
class PublicKeySerializer : KSerializer<PublicKey> {

    override fun deserialize(input: Decoder): PublicKey {
        return when (input) {
            is BinaryDecoder -> CryptoService.loadPublicKey(
                input.decodeByteArray()
            )
            else -> CryptoService.loadPublicKey(
                Utils.HEX.decode(
                    input.decodeString()
                )
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
                Utils.HEX.encode(
                    CryptoService.encodePublicKey(
                        obj
                    )
                )
            )
        }
    }
}
