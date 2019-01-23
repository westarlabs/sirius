package org.starcoin.sirius.core

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.crypto.eth.toECDSASignature

class SiriusObjectSerializationTest2 : SiriusObjectSerializationTest() {

    @Test
    fun testEthereumSignature() {
        Assert.assertNotNull(Signature.ZERO_SIGN.toECDSASignature())
    }
}
