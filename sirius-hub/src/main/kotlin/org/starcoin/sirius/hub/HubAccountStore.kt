package org.starcoin.sirius.hub

import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.HubAccount
import org.starcoin.sirius.datasource.DataSource

class HubAccountStore(val dataSource: DataSource<ByteArray, ByteArray>) {

    fun getAccount(address: Address): HubAccount? {
        return dataSource.get(address.toBytes())?.let { HubAccount.parseFromProtobuf(it) }
    }


}