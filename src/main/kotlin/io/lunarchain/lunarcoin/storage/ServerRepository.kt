package io.lunarchain.lunarcoin.storage

import io.lunarchain.lunarcoin.config.BlockChainConfig
import io.lunarchain.lunarcoin.core.*
import io.lunarchain.lunarcoin.serialization.*
import io.lunarchain.lunarcoin.trie.PatriciaTrie
import io.lunarchain.lunarcoin.util.*
import lunar.vm.DataWord
import java.math.BigInteger

/**
 * 管理区块链状态的管理类(Account State, Blocks和Transactions)，不同的BlockChainConfig应该使用不同的Repository。
 */
class ServerRepository : Repository {

    var config: BlockChainConfig

    /**
     * 不允许直接构造Repository。
     */
    constructor(config: BlockChainConfig) {
        this.config = config
    }

    companion object {
        /**
         * Repository索引表。
         */
        val repositoryMap = mutableMapOf<BlockChainConfig, ServerRepository>()

        /**
         * 如果反复构造Repository会造成底层数据库重复初始化而出错（例如LevelDb），因此不同的BlockChainConfig应该使用不同的Repository实例。
         */
        fun getInstance(config: BlockChainConfig): Repository {
            if (repositoryMap[config] == null) {
                val rep = ServerRepository(config)
                repositoryMap.put(config, rep)
                return rep
            } else {
                return repositoryMap[config]!!
            }
        }
    }

    /**
     * Account state Db.
     */
    private var accountStateDs: PatriciaTrie? = null

    /**
     * Accounts Db.
     */
    private var accountDs: DataSource<ByteArray, ByteArray>? = null


    /**
     * Accounts storage Db.
     */

    private var accountStorageDs: DataSource<ByteArray,ByteArray>? = null

    /**
     * Blocks Db.
     */
    private var blockDs: ObjectStore<Block>? = null

    /**
     * Block index db.
     */
    private var blockIndexDs: ObjectStore<List<BlockInfo>>? = null

    /**
     * Transactions Db.
     */
    private var transactionDs: ObjectStore<Transaction>? = null

    /**
     * contractCode db
     */

    private var codeDs: DataSource<ByteArray, ByteArray>? = null

    val BEST_BLOCK_KEY = "0".toByteArray()

    /**
     * Best block Db.
     */
    private var bestBlockDs: ObjectStore<Block>? = null

    /**
     * Account State的存储类组装。
     */
    private fun getAccountStateStore(): PatriciaTrie? {
        if (accountStateDs != null) return accountStateDs

        val dbName = "accountState"
        var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
        if (config.getDatabaseType().equals(BlockChainConfig.DatabaseType.LEVELDB.name, true)) {
            ds = LevelDbDataSource(dbName, config.getDatabaseDir())
        }
        ds.init()
        accountStateDs = PatriciaTrie(ds)
        return accountStateDs
    }

    /**
     * Account的存储类组装。
     */
    private fun getAccountStore(password: String): ObjectStore<AccountWithKey>? {
        if (accountDs == null) {
            val dbName = "account"
            var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
            if (config.getDatabaseType().equals(BlockChainConfig.DatabaseType.LEVELDB.name, true)) {
                ds = LevelDbDataSource(dbName, config.getDatabaseDir())
            }
            ds.init()
            accountDs = ds

            return ObjectStore(ds, AccountSerialize(password))
        } else {
            return ObjectStore(accountDs!!, AccountSerialize(password))
        }
    }

    /**
     * Contract Storage的存储类组装。
     */

    private fun getCodeStorage(): DataSource<ByteArray, ByteArray>? {
        if(codeDs == null) {
            val dbName = "codeStorage"
            var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
            if (config.getDatabaseType().equals(BlockChainConfig.DatabaseType.LEVELDB.name, true)) {
                ds = LevelDbDataSource(dbName, config.getDatabaseDir())
            }
            ds.init()
            codeDs = ds
            return ds
        } else {
            return codeDs
        }
    }

    /**
     * Account Storage的存储类组装。
     */

    private fun getAccountStorage(): ObjectStore<AccountStorage>? {
        if (accountStorageDs == null) {
            val dbName = "accountStorage"
            var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
            if (config.getDatabaseType().equals(BlockChainConfig.DatabaseType.LEVELDB.name, true)) {
                ds = LevelDbDataSource(dbName, config.getDatabaseDir())
            }
            ds.init()
            accountStorageDs = ds
            return ObjectStore(ds, StorageSerialize())
        } else {
            return ObjectStore(accountStorageDs!!, StorageSerialize())
        }
    }



    /**
     * Block的存储类组装。
     */
    private fun getBlockStore(): ObjectStore<Block>? {
        if (blockDs != null) return blockDs

        val dbName = "block"
        var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
        if (config.getDatabaseType().equals(BlockChainConfig.DatabaseType.LEVELDB.name, true)) {
            ds = LevelDbDataSource(dbName, config.getDatabaseDir())
        }
        ds.init()
        blockDs = ObjectStore(ds, BlockSerialize())
        return blockDs
    }

    /**
     * Transaction的存储类组装。
     */
    private fun getTransactionStore(): ObjectStore<Transaction>? {
        if (transactionDs != null) return transactionDs

        val dbName = "transaction"
        var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
        if (config.getDatabaseType().equals(BlockChainConfig.DatabaseType.LEVELDB.name, true)) {
            ds = LevelDbDataSource(dbName, config.getDatabaseDir())
        }
        ds.init()
        transactionDs = ObjectStore<Transaction>(ds, TransactionSerialize())
        return transactionDs
    }

    /**
     * Block Index的存储类组装。
     */
    private fun getBlockIndexStore(): ObjectStore<List<BlockInfo>>? {
        if (blockIndexDs != null) return blockIndexDs

        val dbName = "blockIndex"
        var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
        if (config.getDatabaseType().equals(BlockChainConfig.DatabaseType.LEVELDB.name, true)) {
            ds = LevelDbDataSource(dbName, config.getDatabaseDir())
        }
        ds.init()
        blockIndexDs = ObjectStore(ds, BlockInfosSerialize())
        return blockIndexDs
    }

    /**
     * BestBlock的存储类组装。
     */
    private fun getBestBlockStore(): ObjectStore<Block>? {
        if (bestBlockDs != null) return bestBlockDs

        val dbName = "bestBlock"
        var ds: DataSource<ByteArray, ByteArray> = MemoryDataSource(dbName)
        if (config.getDatabaseType().equals(BlockChainConfig.DatabaseType.LEVELDB.name, true)) {
            ds = LevelDbDataSource(dbName, config.getDatabaseDir())
        }
        ds.init()
        bestBlockDs = ObjectStore(ds, BlockSerialize())
        return bestBlockDs
    }

    /**
     * 读取账户余额。
     */
    override fun getBalance(address: ByteArray): BigInteger {
        return getAccountState(address)?.balance ?: BigInteger.ZERO
    }

    /**
     * 账户的Nonce+1
     */
    override fun increaseNonce(address: ByteArray) {
        val accountState = getOrCreateAccountState(address)
        getAccountStateStore()?.update(
            address,
            CodecUtil.encodeAccountState(accountState.increaseNonce())
        )
    }

    /**
     * 增加账户余额。
     */
    override fun addBalance(address: ByteArray, amount: BigInteger) {
        val accountState = getOrCreateAccountState(address)
        getAccountStateStore()?.update(
            address,
            CodecUtil.encodeAccountState(accountState.increaseBalance(amount))
        )
    }

    override fun saveAccount(account: AccountWithKey, password: String): Int {
        val ds = getAccountStore(password)
        val index = ds?.keys()?.size ?: 0
        val key = index.toString(10).toByteArray()
        ds?.put(key, account)
        return index
    }

    override fun getAccount(index: Int, password: String): AccountWithKey? {
        val ds = getAccountStore(password)
        val key = index.toString(10).toByteArray()
        val account = ds?.get(key)
        account?.index = index
        return account
    }

    override fun accountNumber(): Int {
        val ds = getAccountStore("")
        val keys = ds?.keys()
        return keys?.size ?: 0
    }

    override fun getBlockInfo(hash: ByteArray): BlockInfo? {
        val block = getBlock(hash)
        if (block != null) {
            val blockInfos = getBlockInfos(block.height)
            return blockInfos?.first { it.hash.contentEquals(block.hash) }
        } else {
            return null
        }
    }

    override fun getBlockInfos(height: Long): List<BlockInfo>? {
        return getBlockIndexStore()?.get(CodecUtil.longToByteArray(height))
    }

    override fun getBlock(hash: ByteArray): Block? {
        return getBlockStore()?.get(hash)
    }

    override fun saveBlock(block: Block) {
        getBlockStore()?.put(block.hash, block)
    }

    override fun getBestBlock(): Block? {
        return getBestBlockStore()?.get(BEST_BLOCK_KEY)
    }

    override fun updateBestBlock(block: Block) {
        getBestBlockStore()?.put(BEST_BLOCK_KEY, block)
    }

    override fun putTransaction(trx: Transaction) {
        getTransactionStore()?.put(trx.hash(), trx)
    }

    override fun close() {
        accountDs?.close()
        accountStateDs?.close()
        transactionDs?.close()
        blockDs?.close()
        blockIndexDs?.close()
    }

    /**
     * 新建账户。
     */
    fun createAccountState(address: ByteArray): AccountState {
        val state = AccountState(BigInteger.ZERO, BigInteger.ZERO, HashUtil.EMPTY_TRIE_HASH, HashUtil.EMPTY_DATA_HASH)
        getAccountStateStore()?.update(address, CodecUtil.encodeAccountState(state))
        return state
    }

    /**
     * 新建账户存储空间
     */

    fun createAccountStorage(address: ByteArray) {
        val accountStorage = getAccountStorage()
        val storage = accountStorage!!.get(address)
        if(accountStorage!!.get(address) != null) return
        else accountStorage.put(address, AccountStorage(address,HashMap()))

    }

    /**
     * 判断账户状态Account State是不是存在，如果不存在就新建账户。
     */
    override fun getOrCreateAccountState(address: ByteArray): AccountState {
        var ret = getAccountState(address)
        if (ret == null) {
            ret = createAccountState(address)
        }
        createAccountStorage(address)
        return ret
    }

    override fun getAccountState(address: ByteArray) = getAccountStateStore()?.get(
        address
    )?.let { CodecUtil.decodeAccountState(it) }

    override fun changeAccountStateRoot(stateRoot: ByteArray) {
        getAccountStateStore()?.changeRoot(stateRoot)
    }

    override fun getAccountStateRoot(): ByteArray? {
        return getAccountStateStore()?.rootHash
    }

    override fun isExist(address: ByteArray): Boolean {
        return getAccountState(address) != null
    }

    override fun getStorageValue(addr: ByteArray, key: DataWord): DataWord? {
        val accountState = getAccountState(addr)
        if(accountState == null) return null
        else {
            val accountStorage = getAccountStorage()
            val objStorage = accountStorage!!.get(addr)!!.storage
            return objStorage[key]
        }

    }



    override fun getCodeHash(addr: ByteArray): ByteArray? {
        val accountState = getAccountState(addr)
        return if (accountState != null) accountState.getCodeHash() else HashUtil.EMPTY_DATA_HASH
    }

    override fun saveCode(addr: ByteArray, code: ByteArray) {
        val codeHash = CryptoUtil.sha3(code)
        getCodeStorage()!!.put(codeHash, code)
        val accountState = getOrCreateAccountState(addr)
        getAccountStateStore()?.update(addr, CodecUtil.encodeAccountState(accountState.withCodeHash(codeHash)))


    }


    override fun getCode(addr: ByteArray): ByteArray? {
        val codeHash = getCodeHash(addr)
        if(codeHash == null) return HashUtil.EMPTY_DATA_HASH
        return if (FastByteComparisons.equal(codeHash!!, HashUtil.EMPTY_DATA_HASH))
            ByteUtil.EMPTY_BYTE_ARRAY
        else {
            return getCodeStorage()!!.get(codeHash)
        }
    }

    override fun getBlockHashByNumber(blockNumber: Long, branchBlockHash: ByteArray): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addStorageRow(addr: ByteArray, key: DataWord, value: DataWord) {
        getOrCreateAccountState(addr)
        val accountStorage = getAccountStorage()
        val storageObj = accountStorage!!.get(addr)
        storageObj!!.storage.put(key, value)
        val newStorageObj = storageObj!!.storage
        accountStorage.put(addr, AccountStorage(addr, newStorageObj))

    }


    override fun updateBlockInfo(height: Long, blockInfo: BlockInfo) {
        val blockIndexStore = getBlockIndexStore()
        val k = CodecUtil.longToByteArray(height)
        val blockInfoList = blockIndexStore?.get(k)
        if (blockInfoList != null) {
            val filtered = blockInfoList.dropWhile { it.hash.contentEquals(blockInfo.hash) }
            val converted = filtered.map { BlockInfo(it.hash, false, it.totalDifficulty) }
            blockIndexStore.put(CodecUtil.longToByteArray(height), converted.plus(blockInfo))
        } else {
            blockIndexStore?.put(CodecUtil.longToByteArray(height), listOf(blockInfo))
        }
    }
}
