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

    override fun deserialize(decoder: Decoder): PrivateKey {
        return when (decoder) {
            is BinaryDecoder -> CryptoService.loadPrivateKey(
                decoder.decodeByteArray()
            )
            else -> CryptoService.loadPrivateKey(
                decoder.decodeString().hexToByteArray()
            )
        }
    }

    override fun serialize(encoder: Encoder, obj: PrivateKey) {
        when (encoder) {
            is BinaryEncoder -> encoder.encodeByteArray(
                CryptoService.encodePrivateKey(
                    obj
                )
            )
            else -> encoder.encodeString(
                CryptoService.encodePrivateKey(
                    obj
                ).toHEXString()
            )
        }
    }
}
