package org.starcoin.sirius.core

import com.google.common.collect.Lists
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.util.MockUtils
import java.util.*

class AMTreeTest {

    @Test
    fun testAMTree() {
        val eon = 1
        val accounts = ArrayList<HubAccount>()

        val count = 4

        for (i in 0 until count) {
            val a = HubAccount(
                CryptoService.generateCryptoKey().keyPair.public, Update(eon, 0, 5, 10, Hash.random()), 5
            )
            accounts.add(a)
        }

        val tree = AMTree(eon, accounts)
        Assert.assertNotNull(tree.hash())
        Assert.assertEquals(0, tree.offset)
        Assert.assertEquals(10 * count.toLong(), tree.allotment)
    }

    //TODO random exception org.starcoin.sirius.core.AMTreePathLeafNode cannot be cast to org.starcoin.sirius.core.AMTreePathInternalNode
    @Test
    fun testMembershipProof() {
        val tree = AMTree.random(MockUtils.nextInt(10, 1000))
        val node = tree.randomLeafNode()!!
        val path = tree.getMembershipProof((node.info as AMTreeLeafNodeInfo).addressHash)
        Assert.assertTrue(AMTree.verifyMembershipProof(tree.root, path))
    }


    @Test
    fun testEmptyListHash() {
        val tree0 = AMTree(0, mutableListOf())
        val tree1 = AMTree(0, mutableListOf())

        Assert.assertEquals(tree0.hash(), tree1.hash())
    }

    @Test
    fun testEmptyListRoot() {
        val tree0 = AMTree(0, mutableListOf())
        val tree1 = AMTree(0, mutableListOf())
        Assert.assertNotNull(tree0.root)
        Assert.assertNotNull(tree0.root.info)
        Assert.assertEquals(tree0.root, tree1.root)
    }

    @Test
    fun testSingleNode() {
        val eon = 0
        val a = HubAccount.mock()
        val tree = AMTree(eon, Lists.newArrayList(a))
        val proof = tree.getMembershipProof(a.address)!!
        Assert.assertTrue(AMTree.verifyMembershipProof(tree.root, proof))

        // test verify after marshal
        val rootBytes = (tree.root.toAMTreePathNode() as AMTreePathInternalNode).toProtobuf()
        val root = AMTreePathInternalNode.parseFromProtobuf(rootBytes)
        Assert.assertEquals(tree.root.toAMTreePathNode(), root)

        val proof1 = AMTreeProof.parseFromProtobuf(proof.toProtobuf())
        Assert.assertEquals(proof, proof1)
        Assert.assertTrue(AMTree.verifyMembershipProof(root, proof1))
    }
}
