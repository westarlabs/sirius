package org.starcoin.sirius.eth.core

import org.apache.commons.lang3.RandomUtils
import org.ethereum.util.blockchain.EtherUtil
import org.junit.Assert
import org.junit.Test

class EtherUnitTest {

    @Test
    fun testBigDecimal() {
        //kotlin BigDecimal div use RoundingMode.HALF_EVEN
        Assert.assertEquals(1.toBigDecimal() / 2.toBigDecimal(), 0.toBigDecimal())
        Assert.assertEquals(1.toBigDecimal().divide(2.toBigDecimal()), 0.5.toBigDecimal())
    }

    @Test
    fun testConvert() {
        Assert.assertEquals(1.ether, 1.finney * 1000)
        Assert.assertEquals(1.ether, 1.ether.inEther)
        Assert.assertEquals(1.ether, 1.ether.inWei.inEther)
        Assert.assertEquals(1.ether, 1.ether.inFinney.inSzabo.inGWei.inWei.inEther)
    }

    @Test
    fun testBestUnit() {
        var enumber = 1.ether + 1.finney
        Assert.assertEquals(1001.finney, enumber)
        Assert.assertEquals(EtherUnit.Finney, enumber.unit)
        enumber += 999.finney
        Assert.assertEquals(2000.finney, enumber)
        Assert.assertEquals(EtherUnit.Ether, enumber.unit)
    }

    @Test
    fun testEtherUnit() {
        Assert.assertEquals(1.ether, EtherUtil.convert(1, EtherUtil.Unit.ETHER).wei)
        Assert.assertEquals(2.ether + 3.ether, EtherUtil.convert(5, EtherUtil.Unit.ETHER).wei)
        Assert.assertEquals(10.ether.inWei.value, EtherUtil.convert(10, EtherUtil.Unit.ETHER))
        Assert.assertEquals(
            Integer.MAX_VALUE.ether,
            EtherUtil.convert(Integer.MAX_VALUE.toLong(), EtherUtil.Unit.ETHER).wei
        )
        //println(10.ether)
    }

    @Test
    fun testFuzzyEquals() {
        val random = RandomUtils.nextLong(1, EtherUnit.Gwei.radio.longValueExact())
        Assert.assertTrue((1.ether - random.wei).fuzzyEquals(1.ether, EtherUnit.Gwei))
    }

    @Test
    fun testMath() {
        Assert.assertEquals(1.ether + 1.wei, 1.wei + 1.ether)
        Assert.assertEquals(EtherUnit.Ether.radio + 1.toBigInteger(), (1.wei + 1.ether).inWei.value)
        Assert.assertEquals(EtherUnit.Ether.radio - 1.toBigInteger(), (1.ether - 1.wei).inWei.value)
        Assert.assertEquals(1.finney, 1.ether / 1000)
    }
}
