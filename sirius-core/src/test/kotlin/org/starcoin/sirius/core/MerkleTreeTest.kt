package org.starcoin.sirius.core

import com.google.common.collect.Lists
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.util.MockUtils
import java.util.*

class MerkleTreeTest {

    class HashObject(internal var data: ByteArray) : Hashable {

        override fun hash(): Hash {
            return Hash.of(data)
        }
    }

    @Test
    fun testMerkleTree() {

        val leaves = MockUtils.nextInt(1, 100)

        val tests = ArrayList<Hashable>()
        val tests2 = ArrayList<Hashable>()
        for (i in 0 until leaves) {
            val `object` = HashObject(
                RandomStringUtils.randomAlphabetic(MockUtils.nextInt(10, 100)).toByteArray()
            )
            tests.add(`object`)
            tests2.add(`object`)
        }

        val tree = MerkleTree(tests)
        val tree2 = MerkleTree(tests2)

        Assert.assertNotNull(tree.hash())
        Assert.assertEquals(tree.hash(), tree2.hash())
    }

    @Test
    fun testMembershipProof() {
        val tree = generateRandomTree(MockUtils.nextInt(10, 1000))
        val node = tree.randomLeafNode()
        val path = tree.getMembershipProof(node?.hash())
        Assert.assertTrue(MerkleTree.verifyMembershipProof(tree.getRoot().hash(), path, node?.data))
    }

    @Test
    fun testMembershipProofNull() {
        val tree = generateRandomTree(MockUtils.nextInt(10, 1000))
        val node = tree.randomLeafNode()
        val path = tree.getMembershipProof(node?.hash())
        Assert.assertFalse(MerkleTree.verifyMembershipProof(tree.getRoot().hash(), path, null))
        Assert.assertFalse(MerkleTree.verifyMembershipProof(tree.getRoot().hash(), null, node?.data))
        Assert.assertFalse(MerkleTree.verifyMembershipProof(null as Hash?, path, node?.data))
    }

    private fun generateRandomTree(count: Int): MerkleTree {
        val eon = 1
        val txs = ArrayList<OffchainTransaction>()

        for (i in 0 until count) {
            val tx = OffchainTransaction(
                eon, Address.random(), Address.random(), MockUtils.nextLong()
            )
            txs.add(tx)
        }

        return MerkleTree(txs)
    }

//    @Test
//    @Throws(InvalidProtocolBufferException::class)
//    fun testCodec() {
//        val tree = generateRandomTree(MockUtils.nextInt(10, 1000))
//        val node = tree.randomLeafNode()
//        val path = tree.getMembershipProof(node.hash())
//        val bytes = path.toProto().toByteString()
//
//        val path2 = MerklePath(ProtoMerklePath.parseFrom(bytes))
//        Assert.assertEquals(path, path2)
//    }

    @Test
    fun testSingleNode() {
        val eon = 0
        val tx = OffchainTransaction(
            eon, Address.random(), Address.random(), MockUtils.nextLong()
        )
        val tree = MerkleTree(Lists.newArrayList(tx))
        val path = tree.getMembershipProof(tx.hash())
        Assert.assertTrue(MerkleTree.verifyMembershipProof(tree.hash(), path, tx))
    }
}
