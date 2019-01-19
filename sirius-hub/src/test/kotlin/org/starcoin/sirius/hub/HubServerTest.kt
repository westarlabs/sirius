package org.starcoin.sirius.hub

import com.google.protobuf.Empty
import io.grpc.inprocess.InProcessChannelBuilder
import org.ethereum.util.blockchain.EtherUtil
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.starcoin.proto.HubServiceGrpc
import org.starcoin.proto.Starcoin
import org.starcoin.sirius.core.Participant
import org.starcoin.sirius.core.Update
import org.starcoin.sirius.crypto.CryptoService
import org.starcoin.sirius.protocol.EthereumTransaction
import org.starcoin.sirius.protocol.ethereum.EthereumAccount
import org.starcoin.sirius.protocol.ethereum.InMemoryChain
import org.starcoin.sirius.util.WithLogging
import kotlin.properties.Delegates

class HubServerTest {
    companion object : WithLogging()

    var hubServer: HubServer<EthereumTransaction,EthereumAccount> by Delegates.notNull()
    val configuration = Configuration.configurationForUNIT()

    @Before
    fun before() {

        val chain = InMemoryChain()
        val owner = EthereumAccount(configuration.ownerKey)
        chain.miningCoin(owner.address, EtherUtil.convert(Int.MAX_VALUE.toLong(), EtherUtil.Unit.ETHER))

        hubServer = HubServer(configuration,chain,owner)
        hubServer.start()
    }

    @Test
    fun testGrpc() {
        val stub =
            HubServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName(configuration.rpcBind.toString()).build())
        waitHubReady(stub)
        val key = CryptoService.generateCryptoKey()
        val participant = Participant(key.keyPair.public)
        val update = Update()
        update.sign(key)
        val serverUpdate = Update.parseFromProtoMessage(
            stub.registerParticipant(
                Starcoin.RegisterParticipantRequest.newBuilder()
                    .setParticipant(participant.toProto<Starcoin.Participant>())
                    .setUpdate(update.toProto<Starcoin.Update>()).build()
            )
        )
        Assert.assertTrue(serverUpdate.isSignedByHub)
        Assert.assertEquals(update.data, serverUpdate.data)
    }

    fun waitHubReady(stub: HubServiceGrpc.HubServiceBlockingStub) {
        var hubInfo = stub.getHubInfo(Empty.newBuilder().build())
        while (!hubInfo.ready) {
            LOG.info("waiting hub ready:")
            Thread.sleep(100)
            hubInfo = stub.getHubInfo(Empty.newBuilder().build())
        }
    }

    @After
    fun after() {
        hubServer.stop()
    }
}
