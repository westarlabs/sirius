package org.starcoin.sirius.contract.test

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.core.*
import org.starcoin.sirius.serialization.rlp.RLP
import kotlin.random.Random

class SiriusModelTest : ContractTestBase("model_test.sol", "test_all") {

    @Test
    fun testHubRoot() {
        val eon: Int = Random(Int.MAX_VALUE).nextInt()
        val offset:Long = 0
        val allotment:Long = Random(Long.MAX_VALUE).nextLong()
        val info = AMTreeInternalNodeInfo(Hash.random(), offset, Hash.random())
        var root = AMTreePathInternalNode(info, Direction.ROOT, offset, allotment)
        val hub = HubRoot(root, eon)

        val data = RLP.dump(HubRoot.serializer(), hub)

        val callResult = contract.callFunction("hub_root_test", data)
        callResult.receipt.logInfoList.forEach { logInfo ->
            println("event:$logInfo")
        }
    }
}