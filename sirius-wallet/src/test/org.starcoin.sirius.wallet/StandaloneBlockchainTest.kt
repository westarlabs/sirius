package org.starcoin.sirius.wallet


import org.ethereum.config.SystemProperties
import org.ethereum.crypto.ECKey
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test
import org.spongycastle.util.encoders.Hex

import java.math.BigInteger

import org.ethereum.util.blockchain.EtherUtil.Unit.ETHER
import org.ethereum.util.blockchain.EtherUtil.convert

/**
 * Created by Anton Nashatyrev on 06.07.2016.
 */
 class StandaloneBlockchainTest {

@Test
 fun constructorTest() {
val sb = StandaloneBlockchain().withAutoblock(true)
val a = sb.submitNewContract(
        "contract A {" +
        "  uint public a;" +
        "  uint public b;" +
        "  function A(uint a_, uint b_) {a = a_; b = b_; }" +
        "}",
"A", 555, 777
)
Assert.assertEquals(BigInteger.valueOf(555), a.callConstFunction("a")[0])
Assert.assertEquals(BigInteger.valueOf(777), a.callConstFunction("b")[0])

val b = sb.submitNewContract(
("contract A {" +
"  string public a;" +
"  uint public b;" +
"  function A(string a_, uint b_) {a = a_; b = b_; }" +
"}"),
"A", "This string is longer than 32 bytes...", 777
)
Assert.assertEquals("This string is longer than 32 bytes...", b.callConstFunction("a")[0])
Assert.assertEquals(BigInteger.valueOf(777), b.callConstFunction("b")[0])
}

@Test
 fun fixedSizeArrayTest() {
val sb = StandaloneBlockchain().withAutoblock(true)
run { val a = sb.submitNewContract(
        ("contract A {" +
                "  uint public a;" +
                "  uint public b;" +
                "  address public c;" +
                "  address public d;" +
                "  function f(uint[2] arr, address[2] arr2) {a = arr[0]; b = arr[1]; c = arr2[0]; d = arr2[1];}" +
                "}"))
    val addr1 = ECKey()
    val addr2 = ECKey()
    a.callFunction("f", arrayOf(111, 222), arrayOf<ByteArray>(addr1.getAddress(), addr2.getAddress()))
    Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction("a")[0])
    Assert.assertEquals(BigInteger.valueOf(222), a.callConstFunction("b")[0])
    Assert.assertArrayEquals(addr1.getAddress(), a.callConstFunction("c")[0] as ByteArray)
    Assert.assertArrayEquals(addr2.getAddress(), a.callConstFunction("d")[0] as ByteArray) }

    run { val addr1 = ECKey()
        val addr2 = ECKey()
        val a = sb.submitNewContract(
                ("contract A {" +
                        "  uint public a;" +
                        "  uint public b;" +
                        "  address public c;" +
                        "  address public d;" +
                        "  function A(uint[2] arr, address a1, address a2) {a = arr[0]; b = arr[1]; c = a1; d = a2;}" +
                        "}"), "A",
                arrayOf(111, 222), addr1.getAddress(), addr2.getAddress())
        Assert.assertEquals(BigInteger.valueOf(111), a.callConstFunction("a")[0])
        Assert.assertEquals(BigInteger.valueOf(222), a.callConstFunction("b")[0])
        Assert.assertArrayEquals(addr1.getAddress(), a.callConstFunction("c")[0] as ByteArray)
        Assert.assertArrayEquals(addr2.getAddress(), a.callConstFunction("d")[0] as ByteArray)

        val a1 = "0x1111111111111111111111111111111111111111"
        val a2 = "0x2222222222222222222222222222222222222222" }
}

@Test
 fun encodeTest1() {
val sb = StandaloneBlockchain().withAutoblock(true)
val a = sb.submitNewContract(
("contract A {" +
"  uint public a;" +
"  function f(uint a_) {a = a_;}" +
"}"))
a.callFunction("f", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
val r = a.callConstFunction("a")[0] as BigInteger
println(r.toString(16))
Assert.assertEquals(BigInteger(Hex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")), r)
}

@Test
 fun invalidTxTest() {
 // check that invalid tx doesn't break implementation
        val sb = StandaloneBlockchain()
val alice = sb.getSender()
val bob = ECKey()
sb.sendEther(bob.getAddress(), BigInteger.valueOf(1000))
sb.setSender(bob)
sb.sendEther(alice.getAddress(), BigInteger.ONE)
sb.setSender(alice)
sb.sendEther(bob.getAddress(), BigInteger.valueOf(2000))

sb.createBlock()
}

@Test
 fun initBalanceTest() {
 // check StandaloneBlockchain.withAccountBalance method
        val sb = StandaloneBlockchain()
val alice = sb.getSender()
val bob = ECKey()
sb.withAccountBalance(bob.getAddress(), convert(123, ETHER))

val aliceInitBal = sb.getBlockchain().getRepository().getBalance(alice.getAddress())
val bobInitBal = sb.getBlockchain().getRepository().getBalance(bob.getAddress())
assert(convert(123, ETHER) == bobInitBal)

sb.setSender(bob)
sb.sendEther(alice.getAddress(), BigInteger.ONE)

sb.createBlock()

assert(convert(123, ETHER).compareTo(sb.getBlockchain().getRepository().getBalance(bob.getAddress())) > 0)
assert(aliceInitBal.add(BigInteger.ONE) == sb.getBlockchain().getRepository().getBalance(alice.getAddress()))
}

companion object {

@AfterClass
 fun cleanup() {
SystemProperties.resetToDefault()
}
}

}