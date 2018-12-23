package org.starcoin.sirius.core

import com.google.common.collect.Lists
import com.google.protobuf.InvalidProtocolBufferException
import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Test
import org.starcoin.proto.Starcoin.ProtoAugmentedMerklePath
import org.starcoin.proto.Starcoin.ProtoAugmentedMerkleTreeNode
import java.util.*

class AugmentedMerkleTreeTest {

    @Test
    fun testAugmentedMerkleTree() {
        val eon = 1
        val accountInformationList = ArrayList<AccountInformation>()

        val count = 4

        for (i in 0 until count) {
            val a = AccountInformation(
                Address.random(), 5, Update(eon, 0, 5, 10, Hash.random())
            )
            accountInformationList.add(a)
        }

        val tree = AugmentedMerkleTree(eon, accountInformationList)
        Assert.assertNotNull(tree.hash())
        Assert.assertEquals(0, tree.offset)
        Assert.assertEquals(5 * count.toLong(), tree.allotment)
    }

    @Test
    fun testMembershipProof() {
        val tree = generateRandomTree(RandomUtils.nextInt(10, 1000))
        val node = tree.randomLeafNode()
        val path = tree.getMembershipProof(node.account!!.address)
        Assert.assertTrue(AugmentedMerkleTree.verifyMembershipProof(tree.root!!, path!!))
    }

    private fun generateRandomTree(count: Int): AugmentedMerkleTree {
        return AugmentedMerkleTree.random(count)
    }

    @Test
    fun testEmptyListHash() {
        val tree0 = AugmentedMerkleTree(0, mutableListOf())
        val tree1 = AugmentedMerkleTree(0, mutableListOf())

        Assert.assertEquals(tree0.hash(), tree1.hash())
    }

    @Test
    fun testEmptyListRoot() {
        val tree0 = AugmentedMerkleTree(0, mutableListOf())
        val tree1 = AugmentedMerkleTree(0, mutableListOf())
        Assert.assertNotNull(tree0.root)
        Assert.assertNotNull(tree0.root!!.information)
        Assert.assertEquals(tree0.root, tree1.root)
    }

    @Test
    @Throws(InvalidProtocolBufferException::class)
    fun testSingleNode() {
        val eon = 0
        val address = Address.random()
        val a = AccountInformation(address, 0, Update(eon, 0, 0, 0))
        val tree = AugmentedMerkleTree(eon, Lists.newArrayList(a))
        val path = tree.getMembershipProof(address)
        Assert.assertTrue(AugmentedMerkleTree.verifyMembershipProof(tree.root!!, path))

        // test verify after marshal
        val byteStringRoot = tree.root!!.toProto().toByteString()
        val root = AugmentedMerkleTree.AugmentedMerkleTreeNode(ProtoAugmentedMerkleTreeNode.parseFrom(byteStringRoot))
        Assert.assertEquals(tree.root, root)
        val path1 = AugmentedMerklePath(ProtoAugmentedMerklePath.parseFrom(path.toProto().toByteString()))
        Assert.assertEquals(path, path1)
        Assert.assertTrue(AugmentedMerkleTree.verifyMembershipProof(root, path1))
    }
}
