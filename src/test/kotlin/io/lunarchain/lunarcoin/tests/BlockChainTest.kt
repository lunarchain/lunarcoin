package io.lunarchain.lunarcoin.tests

import io.lunarchain.lunarcoin.config.BlockChainConfig
import io.lunarchain.lunarcoin.core.*
import io.lunarchain.lunarcoin.miner.BlockMiner
import io.lunarchain.lunarcoin.storage.ServerRepository
import io.lunarchain.lunarcoin.util.BlockChainUtil
import io.lunarchain.lunarcoin.util.CodecUtil
import io.lunarchain.lunarcoin.util.CryptoUtil
import io.lunarchain.lunarcoin.util.CryptoUtil.Companion.generateKeyPair
import io.lunarchain.lunarcoin.util.CryptoUtil.Companion.sha256
import io.lunarchain.lunarcoin.util.CryptoUtil.Companion.verifyTransactionSignature
import io.lunarchain.lunarcoin.vm.program.invoke.ProgramInvokeFactoryImpl
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.util.ASN1Dump
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull


class BlockChainTest {
    val config = BlockChainConfig("application-1.conf")

    val repository = ServerRepository.getInstance(config)

    // 初始化矿工Charlie账户
    val kp3 = generateKeyPair()
    val charlie = Account(kp3.public)


    @Before
    fun setup() {
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        config.setMinerCoinbase(charlie.address)
    }
    /*
    @After
    fun close() {
        repository.close()
    }
    */


/*
    /**
     * 验证账户地址长度为40(20个byte)。
     */
    @Test
    fun validateAddressTest() {
        val keyPair = generateKeyPair() 

        val account = Account(keyPair.public)
        assert(account.address.size == 20)
    }

    /**
     * 验证交易完成后账户余额(balance)是否正确。
     */
    @Test
    fun applyTransactionTest() {


        // 初始化Alice账户
        val kp1 = generateKeyPair()
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = generateKeyPair()
        val bob = Account(kp2.public)

        repository.createAccountState(alice.address)
        repository.createAccountState(bob.address)


        // Alice向Bob转账100
        val trx = Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public, ByteArray(0),
            (repository.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))
        // Alice用私钥签名
        trx.sign(kp1.private)

        val transactionExecutor = TransactionExecutor(repository, config.getGenesisBlock(), trx, 0L, repository, ProgramInvokeFactoryImpl())

        // 初始金额为500
        transactionExecutor.addBalance(alice.address, BigInteger.valueOf(500))
        transactionExecutor.addBalance(bob.address, BigInteger.valueOf(500))

        // 根据交易记录更新区块链状态
        repository.startTracking()
        transactionExecutor.init()
        transactionExecutor.execute()
        transactionExecutor.go()

        // 查询余额是否正确
        assert(repository.getBalance(alice.address) == BigInteger.valueOf(300))
        assert(repository.getBalance(bob.address) == BigInteger.valueOf(600))
    }

    /**
     * 验证ECDSAE签名算法。
     */
    @Test
    fun verifyECDSASignatureTest() {
        // Get the instance of the Key Generator with "EC" algorithm

        val gen = KeyPairGenerator.getInstance("EC", "SC")
        gen.initialize(ECGenParameterSpec("secp256r1"))

        val pair = gen.generateKeyPair()
        // Instance of signature class with SHA256withECDSA algorithm
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(pair.private)

        println("Private Keys is::" + pair.private)
        println("Public Keys is::" + pair.public)

        val msg = "text ecdsa with sha256"//getSHA256(msg)
        signer.update(msg.toByteArray())

        val signature = signer.sign()
        println("Signature is::" + BigInteger(1, signature).toString(16))

        // Validation
        signer.initVerify(pair.public)
        signer.update(msg.toByteArray())
        assert(signer.verify(signature))

    }

    /**
     * 验证交易签名。
     */
    @Test
    fun verifyTransactionSignatureTest() {

        // 初始化Alice账户
        val kp1 = generateKeyPair()
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = generateKeyPair()
        val bob = Account(kp2.public)


        repository.createAccountState(alice.address)
        repository.createAccountState(bob.address)

        // Alice向Bob转账100
        val trx = Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public, ByteArray(0),
            (repository.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))

        // Alice用私钥签名
        val signature = trx.sign(kp1.private)

        // 用Alice的公钥验证交易签名
        assert(verifyTransactionSignature(trx, signature))

        // 验证交易的合法性(签名验证)
        assert(trx.isValid)
    }

    @Test
    fun addressTest() {
        // 初始化Alice账户
        val kp1 = generateKeyPair() 
        val alice = Account(kp1.public)

        assertNotNull(alice.address)

        println(Hex.toHexString(alice.address))
    }

    /**
     * 构造新的区块
     */
    @Test
    fun createBlockTest() {


        val blockChain = BlockChain(config, repository)

        // 初始化Alice账户
        val kp1 = generateKeyPair() 
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = generateKeyPair() 
        val bob = Account(kp2.public)

        repository.createAccountState(alice.address)
        repository.createAccountState(bob.address)


        // Alice向Bob转账100
        val trx = Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public, ByteArray(0),
            (repository.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))
        // Alice用私钥签名
        trx.sign(kp1.private)

        val transactionExecutor = TransactionExecutor(repository, config.getGenesisBlock(), trx, 0L, repository, ProgramInvokeFactoryImpl())

        // 初始金额为500
        transactionExecutor.addBalance(alice.address, BigInteger.valueOf(500))
        transactionExecutor.addBalance(bob.address, BigInteger.valueOf(500))

        val block = blockChain.generateNewBlock(blockChain.getBestBlock(), listOf(trx))
        blockChain.processBlock(block)

        // 查询余额是否正确
        assert(repository.getBalance(alice.address) == BigInteger.valueOf(300))
        assert(repository.getBalance(bob.address) == BigInteger.valueOf(600))
    }

    /**
     * 挖矿算法测试。
     */
    @Test
    fun mineAlgorithmTest() {
        val ver: Int = 1
        val parentHash = "000000000000000117c80378b8da0e33559b5997f2ad55e2f7d18ec1975b9717"
        val merkleRoot = "871714dcbae6c8193a2bb9b2a69fe1c0440399f38d94b3a0f1b447275a29978a"
        val time = 0x53058b35 // 2014-02-20 04:57:25
        val difficulty = 0x1f00ffff // difficulty，比特币的最小(初始)难度为0x1d00ffff，为测试方便我们降低难度为0x1f00ffff

        // 挖矿难度的算法：https://en.bitcoin.it/wiki/Difficulty
        val exp = difficulty shr 24
        val mant = difficulty and 0xffffff
        val target = BigInteger.valueOf(mant.toLong()).multiply(BigInteger.valueOf(2).pow(8 * (exp - 3)))
        val targetStr = "%064x".format(target)
        println("Target:$targetStr")

        var nonce = 0
        while (nonce < 0x100000000) {

            val headerBuffer = ByteBuffer.allocate(4 + 32 + 32 + 4 + 4 + 4)
            headerBuffer.put(ByteBuffer.allocate(4).putInt(ver).array()) // version
            headerBuffer.put(Hex.decode(parentHash)) // parentHash
            headerBuffer.put(Hex.decode(merkleRoot)) // trxTrieRoot
            headerBuffer.put(ByteBuffer.allocate(4).putInt(time).array()) // time
            headerBuffer.put(ByteBuffer.allocate(4).putInt(difficulty).array()) // difficulty(current difficulty)
            headerBuffer.put(ByteBuffer.allocate(4).putInt(nonce).array()) // nonce

            val header = headerBuffer.array()
            val hit = Hex.toHexString(sha256(sha256(header)))
            println("$nonce : $hit")

            if (hit < targetStr) {
                println("Got Nonce : $nonce")
                println("Got Hit : $hit")
                break
            }
            nonce += 1
        }
    }

*/


/*
    /**
     * 挖矿难度(Difficulty)运算测试。
     */
    @Test
    fun difficultyTest() {
        val difficulty = BigInteger.valueOf(0x0404cbL).multiply(BigInteger.valueOf(2).pow(8 * (0x1b - 3)))
        assertEquals(difficulty.toString(16), "404cb000000000000000000000000000000000000000000000000")
    }

    /**
     * Merkle Root Hash测试。
     */
    @Test
    fun merkleTest() {
        // 初始化Alice账户
        val kp1 = generateKeyPair() 
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = generateKeyPair() 
        val bob = Account(kp2.public)

        repository.createAccountState(alice.address)
        repository.createAccountState(bob.address)

        // Alice向Bob转账100
        val trx1 = Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public, ByteArray(0),
            (repository.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))

        // Alice用私钥签名
        trx1.sign(kp1.private)

        // Alice向Bob转账50
        val trx2 = Transaction(alice.address, bob.address, BigInteger.valueOf(50), DateTime(), kp1.public, ByteArray(0),
            (repository.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))


        // Alice用私钥签名
        trx2.sign(kp1.private)

        val trxTrieRoot = CryptoUtil.merkleRoot(listOf(trx1, trx2))
        println(Hex.toHexString(trxTrieRoot))
    }

    /**
     * 挖矿测试
     */
    @Test
    fun mineBlockTest() {
        // 初始化Alice账户

        val blockChain = BlockChain(config, repository)
        val kp1 = generateKeyPair() 
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = generateKeyPair() 
        val bob = Account(kp2.public)

        repository.createAccountState(alice.address)
        repository.createAccountState(bob.address)

        // Alice向Bob转账100
        val trx = Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public, ByteArray(0),
            (repository.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))
        // Alice用私钥签名
        trx.sign(kp1.private)

        val transactionExecutor = TransactionExecutor(repository, config.getGenesisBlock(), trx, 0L, repository, ProgramInvokeFactoryImpl())

        // 初始金额为500
        transactionExecutor.addBalance(alice.address, BigInteger.valueOf(500))
        transactionExecutor.addBalance(bob.address, BigInteger.valueOf(500))

        val block = blockChain.generateNewBlock(blockChain.getBestBlock(), listOf(trx))
        blockChain.processBlock(block)

        // 查询余额是否正确
        assert(repository.getBalance(alice.address) == BigInteger.valueOf(300))
        assert(repository.getBalance(bob.address) == BigInteger.valueOf(600))

        val bestBlock = blockChain.getBestBlock()
        val mineResult = BlockMiner.mine(block, bestBlock.time.millis / 1000, bestBlock.difficulty)
        val totalDifficulty = block.totalDifficulty.add(BigInteger.valueOf(mineResult.difficulty))
        val minedBlock = Block(
            block.version, block.height, block.parentHash, block.coinBase, block.time,
            mineResult.difficulty, mineResult.nonce, totalDifficulty, block.stateRoot, block.trxTrieRoot,
            block.transactions, block.gasLimit
        )

        println("Block nonce: ${minedBlock.nonce}")
        assertNotEquals(minedBlock.difficulty, 0)
        assertNotEquals(minedBlock.nonce, 0)
        assert(BlockChainUtil.validateBlock(minedBlock, bestBlock.time.millis / 1000, bestBlock.difficulty))
    }
*/

    /*

    /**
     * 账户状态序列化/反序列化测试。
     */
    @Test
    fun accountStateEncodeTest() {
        val accountState = AccountState(BigInteger.TEN, BigInteger.TEN)

        println(ASN1Dump.dumpAsString(ASN1InputStream(accountState.encode()).readObject()))

        val decoded = CodecUtil.decodeAccountState(accountState.encode())

        assertEquals(accountState.nonce, decoded?.nonce)
        assertEquals(accountState.balance, decoded?.balance)
    }

    /**
     * 交易Transaction序列化/反序列化测试。
     */
    @Test
    fun transactionEncodeTest() {
        // 初始化Alice账户
        val kp1 = generateKeyPair() 
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = generateKeyPair() 
        val bob = Account(kp2.public)

        repository.createAccountState(alice.address)
        repository.createAccountState(bob.address)

        // Alice向Bob转账100
        val trx = Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public, ByteArray(0),
            (repository.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))
        // Alice用私钥签名
        trx.sign(kp1.private)

        val transactionExecutor = TransactionExecutor(repository, config.getGenesisBlock(), trx, 0L, repository, ProgramInvokeFactoryImpl())

        // 初始金额为500
        transactionExecutor.addBalance(alice.address, BigInteger.valueOf(500))
        transactionExecutor.addBalance(bob.address, BigInteger.valueOf(500))

        println(ASN1Dump.dumpAsString(ASN1InputStream(trx.encode()).readObject()))

        val decoded = CodecUtil.decodeTransaction(trx.encode()) ?: return

        assertArrayEquals(trx.senderAddress, decoded.senderAddress)
        assertArrayEquals(trx.receiverAddress, decoded.receiverAddress)
        assertEquals(trx.amount, decoded.amount)
        assertEquals(trx.time.millis, decoded.time.millis)
        assertEquals(trx.publicKey, decoded.publicKey)
    }

    */

    /**
     * 区块Block序列化/反序列化测试。
     */
    @Test
    fun blockEncodeTest() {
        // 初始化Alice账户
        val kp1 = generateKeyPair() 
        val alice = Account(kp1.public)

        // 初始化Bob账户
        val kp2 = generateKeyPair() 
        val bob = Account(kp2.public)

        repository.createAccountState(alice.address)
        repository.createAccountState(bob.address)

        // Alice向Bob转账100
        val trx1 = Transaction(alice.address, bob.address, BigInteger.valueOf(100), DateTime(), kp1.public, ByteArray(0),
            (repository.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))
        // Alice用私钥签名
        trx1.sign(kp1.private)

        val transactionExecutor = TransactionExecutor(repository, config.getGenesisBlock(), trx1, 0L, repository, ProgramInvokeFactoryImpl())

        // 初始金额为500
        transactionExecutor.addBalance(alice.address, BigInteger.valueOf(500))
        transactionExecutor.addBalance(bob.address, BigInteger.valueOf(500))

        // Alice向Bob转账50
        val trx2 = Transaction(alice.address, bob.address, BigInteger.valueOf(50), DateTime(), kp1.public, ByteArray(0),
            (repository.getAccountState(alice.address)!!.nonce).toByteArray(), 1.toBigInteger().toByteArray(), 100.toBigInteger().toByteArray(), ByteArray(0))


        // Alice用私钥签名
        trx2.sign(kp1.private)

        val blockChain = BlockChain(config, ServerRepository.getInstance(config))
        val block = blockChain.generateNewBlock(blockChain.getBestBlock(), listOf(trx1, trx2))
        blockChain.processBlock(block)

        println(ASN1Dump.dumpAsString(ASN1InputStream(block.encode()).readObject()))

        val decoded = CodecUtil.decodeBlock(block.encode()) ?: return

        assertEquals(block.version, decoded.version)
        assertEquals(block.height, decoded.height)
        assertArrayEquals(block.parentHash, decoded.parentHash)
        assertArrayEquals(block.trxTrieRoot, decoded.trxTrieRoot)
        assertArrayEquals(block.coinBase, decoded.coinBase)
        assertArrayEquals(block.transactions.toTypedArray(), decoded.transactions.toTypedArray())
        assertEquals(block.time, decoded.time)
        assertEquals(block.difficulty, decoded.difficulty)
        assertEquals(block.nonce, decoded.nonce)
        assertArrayEquals(block.hash, decoded.hash)
    }

    @Test
    fun getPublicKeyFromPrivateKeyTest() {
        for (i in 0..100) {
            val kp = generateKeyPair() 
            val privateKey = kp.private
            val publicKey = kp.public

            val generatedPublicKey = CryptoUtil.generatePublicKey(privateKey)

            assertArrayEquals(publicKey.encoded, generatedPublicKey?.encoded)

            println("Pass public key from private key test :$i")
        }
    }


}
