package org.starcoin.sirius.hub

import org.starcoin.sirius.core.AccountEonState
import org.starcoin.sirius.core.Address
import org.starcoin.sirius.core.HubAccount
import org.starcoin.sirius.datastore.*
import org.starcoin.sirius.serialization.PublicKeyCodec
import java.security.PublicKey

class HubAccountStore(
    val eon: Int,
    private val keyStore: ObjectStore<Address, PublicKey>,
    private val eonStateStore: SiriusObjectStore<Address, AccountEonState>
) : DataStore<Address, HubAccount> {


    constructor(
        eon: Int,
        keyStore: DataStore<ByteArray, ByteArray>,
        eonStateStore: DataStore<ByteArray, ByteArray>
    ) : this(
        eon,
        ObjectStore(Address, PublicKeyCodec, keyStore),
        SiriusObjectStore.addressStore(AccountEonState::class, eonStateStore)
    )

    constructor(eon: Int, factory: DataStoreFactory) : this(
        eon,
        factory.getOrCreate("account"),
        factory.getOrCreate("account-$eon")
    )

    override fun put(key: Address, value: HubAccount) {
        this.put(value)
    }

    fun put(hubAccount: HubAccount) {
        keyStore.put(hubAccount.address, hubAccount.publicKey)
        putAccountState(hubAccount.address, hubAccount.eonState)
    }

    private fun putAccountState(address: Address, state: AccountEonState) {
        if (state.isEmpty()) {
            return
        }
        eonStateStore.put(address, state)
    }


    override fun get(key: Address): HubAccount? {
        val publicKey = keyStore.get(key) ?: return null
        val eonState = this.getAccountState(key)
        return HubAccount(publicKey, eonState)
    }

    private fun getAccountState(address: Address): AccountEonState {
        return eonStateStore.get(address) ?: AccountEonState(eon)
    }


    override fun delete(key: Address) {
        this.keyStore.delete(key)
        this.eonStateStore.delete(key)
    }

    override fun flush(): Boolean {
        return this.keyStore.flush() && this.eonStateStore.flush()
    }

    override fun updateBatch(rows: Map<Address, HubAccount>) {
        this.updateBatch(rows.values)
    }

    fun updateBatch(accounts: Iterable<HubAccount>) {
        this.keyStore.updateBatch(accounts.map { Pair(it.address, it.publicKey) }.toMap())
        this.eonStateStore.updateBatch(accounts.filter { it.eonState.isNotEmpty() }.map {
            Pair(
                it.address,
                it.eonState
            )
        }.toMap())
    }

    override fun keys(): List<Address> {
        return this.keyStore.keys()
    }

    override fun iterator(): CloseableIterator<Pair<Address, HubAccount>> {
        return this.keyStore.iterator().map { Pair(it.first, HubAccount(it.second, getAccountState(it.first))) }
    }

    override fun destroy() {
        throw UnsupportedOperationException("directly call origin datasource")
    }

    override fun init() {
        throw UnsupportedOperationException("directly call origin datasource")
    }

    fun asHubAccountIterable(): Iterable<HubAccount> {
        return this.map { it.second }
    }

}

