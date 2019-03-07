package org.starcoin.sirius.serialization

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.crypto.encode
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import java.security.PublicKey

@Serializer(forClass = PublicKey::class)
class PublicKeySerializer : KSerializer<PublicKey> {

    override fun deserialize(decoder: Decoder): PublicKey {
        return when (decoder) {
            is BinaryDecoder -> CryptoService.loadPublicKey(
                decoder.decodeByteArray()
            )
            else -> CryptoService.loadPublicKey(
                decoder.decodeString().hexToByteArray()
            )
        }
    }

    override fun serialize(encoder: Encoder, obj: PublicKey) {
        when (encoder) {
            is BinaryEncoder -> encoder.encodeByteArray(
                CryptoService.encodePublicKey(
                    obj
                )
            )
            else -> encoder.encodeString(
                CryptoService.encodePublicKey(
                    obj
                ).toHEXString()
            )
        }
    }
}

object PublicKeyCodec : Codec<PublicKey> {
    override fun encode(value: PublicKey): ByteArray {
        return value.encode()
    }

    override fun decode(bytes: ByteArray): PublicKey {
        return CryptoService.loadPublicKey(bytes)
    }

}