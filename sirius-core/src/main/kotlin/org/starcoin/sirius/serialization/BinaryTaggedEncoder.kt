package org.starcoin.sirius.serialization

import kotlinx.serialization.TaggedEncoder

abstract class BinaryTaggedEncoder<Tag : Any?>: TaggedEncoder<Tag>(),BinaryEncoder {
}
