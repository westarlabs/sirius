package org.starcoin.sirius.serialization

import kotlinx.serialization.TaggedDecoder

abstract class BinaryTaggedDecoder<Tag:Any?>:TaggedDecoder<Tag>(),BinaryDecoder {
}
