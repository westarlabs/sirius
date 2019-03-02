package org.starcoin.sirius.wallet.core

import io.grpc.Channel
import org.sql2o.Sql2o
import org.starcoin.sirius.core.AMTreeProof
import org.starcoin.sirius.core.InetAddressPort
import org.starcoin.sirius.core.OffchainTransaction
import org.starcoin.sirius.core.Update
import org.starcoin.sirius.datastore.H2DBStore
import org.starcoin.sirius.wallet.core.dao.SiriusObjectDao

class ResourceManager private constructor(){

    lateinit internal var updateDao: SiriusObjectDao<Update>
        private set

    lateinit internal var offchainTransactionDao:SiriusObjectDao<OffchainTransaction>
        private set

    lateinit internal var aMTreeProofDao:SiriusObjectDao<AMTreeProof>
        private set

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

            resourceManager.updateDao = SiriusObjectDao(updateH2Ds,{ Update.parseFromProtobuf(it)})
            resourceManager.offchainTransactionDao = SiriusObjectDao(offchainTransactionH2Ds,{ OffchainTransaction.parseFromProtobuf(it)})
            resourceManager.aMTreeProofDao = SiriusObjectDao(proofH2Ds,{ AMTreeProof.parseFromProtobuf(it)})

            resourceManager.updateDao.init()
            resourceManager.offchainTransactionDao.init()
            resourceManager.aMTreeProofDao.init()

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
