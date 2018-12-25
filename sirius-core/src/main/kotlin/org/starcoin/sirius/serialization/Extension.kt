package org.starcoin.sirius.serialization

import com.google.protobuf.ByteString


fun ByteArray.toByteString() = ByteString.copyFrom(this)!!


