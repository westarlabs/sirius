package org.starcoin.sirius.eth.core

import org.ethereum.util.blockchain.EtherUtil
import org.junit.Assert
import org.junit.Test

class EtherUnitTest {

    @Test
    fun testEtherUnit() {
        Assert.assertEquals(1.ether, EtherUtil.convert(1, EtherUtil.Unit.ETHER).wei)
        Assert.assertEquals(2.ether + 3.ether, EtherUtil.convert(5, EtherUtil.Unit.ETHER).wei)
    }
}
