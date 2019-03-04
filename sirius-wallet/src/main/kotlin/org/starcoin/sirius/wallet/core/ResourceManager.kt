package org.starcoin.sirius.wallet.core

import io.grpc.Channel
import org.sql2o.Sql2o
import org.starcoin.sirius.core.*
import org.starcoin.sirius.datastore.DataStore
import org.starcoin.sirius.datastore.H2DBStore
import org.starcoin.sirius.datastore.SiriusObjectStore

class ResourceManager private constructor(){

    lateinit internal var updateDao: SiriusObjectStore<String,Update>
        private set

    lateinit internal var offchainTransactionDao:SiriusObjectStore<Hash,OffchainTransaction>
        private set

    lateinit internal var aMTreeProofDao:SiriusObjectStore<String,AMTreeProof>
        private set

    lateinit internal var dataStore:DataStore<ByteArray,ByteArray>;

    companion object {
        private val h2databaseUrl = "jdbc:h2:~/.starcoin/liq/%s/data:starcoin;FILE_LOCK=FS;PAGE_SIZE=1024;CACHE_SIZE=819"
        private val h2databaseUrlMemory = H2DBStore.h2dbUrlMemoryFormat

        private val resourceManagerMap :MutableMap<String, ResourceManager> = mutableMapOf()

        lateinit internal var hubChannel: Channel

        internal var isTest: Boolean =true

        private fun initDB(name: String):ResourceManager{
            var url :String?
            if(!isTest)
                url=h2databaseUrl.format(name)
            else
                url= h2databaseUrlMemory.format(name)

            val resourceManager = ResourceManager()

            val sql2o = Sql2o(url,"sa","")

            val updateH2Ds = H2DBStore(sql2o, "update")
            val offchainTransactionH2Ds = H2DBStore(sql2o, "offchain_transaction")
            val proofH2Ds = H2DBStore(sql2o, "proof")

            resourceManager.dataStore = H2DBStore(sql2o,"data")
            resourceManager.updateDao = SiriusObjectStore.stringStore(Update.objClass,updateH2Ds)
            resourceManager.offchainTransactionDao = SiriusObjectStore.hashStore(OffchainTransaction.objClass,offchainTransactionH2Ds)
            resourceManager.aMTreeProofDao = SiriusObjectStore.stringStore(AMTreeProof.objClass,proofH2Ds)


            resourceManager.updateDao.init()
            resourceManager.offchainTransactionDao.init()
            resourceManager.aMTreeProofDao.init()
            resourceManager.dataStore.init()

            resourceManagerMap.put(name,resourceManager)
            return resourceManager
        }

        @Synchronized
        fun instance(name:String):ResourceManager{
            return resourceManagerMap.get(name)?:initDB(name)
        }
    }
}

fun InetAddressPort.toHttpURL():String{
    return String.format("http://%s:%d",this.host,this.port)
}
