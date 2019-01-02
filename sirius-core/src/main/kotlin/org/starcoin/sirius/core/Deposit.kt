package org.starcoin.sirius.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils

@ProtobufSchema(Starcoin.Deposit::class)
@Serializable
data class Deposit(@SerialId(1) val address: Address, @SerialId(2) val amount: Long) : SiriusObject() {

    companion object : SiriusObjectCompanion<Deposit, Starcoin.Deposit>(Deposit::class) {

        override fun mock(): Deposit {
            val key = CryptoService.generateCryptoKey()
            return Deposit(key.address, MockUtils.nextLong())
        }
    }
}
