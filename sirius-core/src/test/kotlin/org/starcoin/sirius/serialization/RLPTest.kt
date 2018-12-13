package org.starcoin.sirius.serialization

import org.junit.Assert
import org.junit.Test
import org.kethereum.functions.rlp.toIntFromRLP
import org.kethereum.functions.rlp.toRLP

class RLPTest {

    @Test
    fun testIntRLP() {
        val int = 1838383984
        val rlp = int.toRLP()
        println(rlp.bytes.size)
        val int1 = rlp.toIntFromRLP()
        Assert.assertEquals(int, int1)
    }
}