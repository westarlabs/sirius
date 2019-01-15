package org.starcoin.sirius.chain

interface ChainStrategyProvider {

    fun createChainStrategy(): ChainStrategy

}
