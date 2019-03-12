package org.starcoin.sirius.wallet.core

import org.starcoin.sirius.protocol.ChainAccount

data class ClientAccount<A: ChainAccount>(val account: A, val name :String){
    internal val address = account.address
    internal val key = account.key
}