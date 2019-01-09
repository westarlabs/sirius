package org.starcoin.sirius.protocol.ethereum

import org.ethereum.core.CallTransaction
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.core.HubRoot
import org.starcoin.sirius.protocol.CommitFunction

class ContractFunctionTest {

    private lateinit var function: CallTransaction.Function

    @Before
    fun setup() {
        val json = """
            {"constant":false,"inputs":[{"name":"data","type":"bytes"}],"name":"commit","outputs":[{"name":"","type":"bool"}],"payable":false,"stateMutability":"nonpayable","type":"function"}
        """
        function = CallTransaction.Function.fromJsonInterface(json)
    }

    @Test
    fun testContractFunctionSignature() {
        val sign = CommitFunction.signature()
        Assert.assertArrayEquals(sign, function.encodeSignature())
    }

    @Test
    fun testContractFunctionEncode() {
        val commit = HubRoot.mock()
        val encode = CommitFunction.encode(commit)
        Assert.assertArrayEquals(function.encode(commit.toRLP()), encode)
    }
}
