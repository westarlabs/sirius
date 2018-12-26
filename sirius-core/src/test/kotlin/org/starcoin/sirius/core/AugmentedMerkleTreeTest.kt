package org.starcoin.sirius.core

import com.google.common.collect.Lists
import org.apache.commons.lang3.RandomUtils
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import java.util.*

class AugmentedMerkleTreeTest {

    @Test
    fun testAugmentedMerkleTree() {
        val eon = 1
        val accounts = ArrayList<HubAccount>()

        val count = 4

        for (i in 0 until count) {
            val a = HubAccount(
                CryptoService.generateCryptoKey().getKeyPair().public, Update(eon, 0, 5, 10, Hash.random()), 5
            )
            accounts.add(a)
        }

        val tree = AugmentedMerkleTree(eon, accounts)
        Assert.assertNotNull(tree.hash())
        Assert.assertEquals(0, tree.offset)
        Assert.assertEquals(5 * count.toLong(), tree.allotment)
    }

    @Test
    fun testMembershipProof() {
        val tree = AugmentedMerkleTree.random(RandomUtils.nextInt(10, 1000))
        val node = tree.randomLeafNode()!!
        val path = tree.getMembershipProof((node.info as AMTLeafNodeInfo).addressHash)
        Assert.assertTrue(AugmentedMerkleTree.verifyMembershipProof(tree.root, path))
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
        Assert.assertNotNull(tree0.root.info)
        Assert.assertEquals(tree0.root, tree1.root)
    }

    @Test
    fun testSingleNode() {
        val eon = 0
        val a = HubAccount.mock()
        val tree = AugmentedMerkleTree(eon, Lists.newArrayList(a))
        val proof = tree.getMembershipProof(a.address)!!
        Assert.assertTrue(AugmentedMerkleTree.verifyMembershipProof(tree.root, proof))

        // test verify after marshal
        val rootBytes = (tree.root.toAMTPathNode() as AMTPathInternalNode).toProtobuf()
        val root = AMTPathInternalNode.parseFromProtobuf(rootBytes)
        Assert.assertEquals(tree.root.toAMTPathNode(), root)

        val proof1 = AMTProof.parseFromProtobuf(proof.toProtobuf())
        Assert.assertEquals(proof, proof1)
        Assert.assertTrue(AugmentedMerkleTree.verifyMembershipProof(root, proof1))
    }
}
