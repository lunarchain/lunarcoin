package io.lunarchain.lunarcoin.tests

import io.lunarchain.lunarcoin.config.BlockChainConfig
import io.lunarchain.lunarcoin.core.*
import io.lunarchain.lunarcoin.network.client.PeerClient
import io.lunarchain.lunarcoin.network.server.PeerServer
import io.lunarchain.lunarcoin.storage.ServerRepository
import io.lunarchain.lunarcoin.util.CryptoUtil
import org.joda.time.DateTime
import org.junit.Test
import java.lang.Thread.sleep
import java.math.BigInteger

class NetworkTest {

    val config = BlockChainConfig.default()

    val repository = ServerRepository.getInstance(config)

    val transactionExecutor = TransactionExecutor(repository)

    @Test
    fun peerClientServerTest() {
        val serverConfig = BlockChainConfig("application-1.conf")

        val clientConfig = BlockChainConfig("application-2.conf")

        val serverChain = BlockChain(serverConfig, ServerRepository.getInstance(serverConfig))

        val clientChain = BlockChain(clientConfig, ServerRepository.getInstance(serverConfig))

        val serverManager = BlockChainManager(serverChain)

        val clientManager = BlockChainManager(clientChain)

        val server = PeerServer(serverManager)
        Thread(Runnable {
            server.start()
        }).start()

        val newTrx = newTransaction()

        serverManager.submitTransaction(newTrx)

        serverManager.startMining()

        sleep(90 * 1000)

        val client = PeerClient(clientManager)
        Thread(Runnable {
            client.connectAsync(Node("", "localhost", config.getPeerListenPort()))
        }).start()

        sleep(90 * 1000)

        server.closeAsync()

        client.closeAsync()

        sleep(10000)
    }

    fun newTransaction(): Transaction {
        // 初始化Alice账户
        val kp1 = CryptoUtil.generateKeyPair()
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = CryptoUtil.generateKeyPair()
        val bob = Account(kp2.public)

        // 初始金额为200
        transactionExecutor.addBalance(alice.address, BigInteger.valueOf(200))
        transactionExecutor.addBalance(bob.address, BigInteger.valueOf(200))

        // Alice向Bob转账100
        val trx = Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public)
        // Alice用私钥签名
        trx.sign(kp1.private)
        return trx
    }

}
