package lunar.storage

import lunar.config.BlockChainConfig
import lunar.core.*
import lunar.serialization.AccountSerialize
import lunar.serialization.BlockInfosSerialize
import lunar.serialization.BlockSerialize
import lunar.serialization.TransactionSerialize
import lunar.trie.PatriciaTrie
import lunar.util.CodecUtil
import java.math.BigInteger

/**
 * 管理区块链状态的管理类(Account State, Blocks和Transactions)，不同的BlockChainConfig应该使用不同的Repository。
 */
class Repository {

  var config: BlockChainConfig

  /**
   * 不允许直接构造Repository。
   */
  private constructor(config: BlockChainConfig) {
    this.config = config
  }

  companion object {
    /**
     * Repository索引表。
     */
    val repositoryMap = mutableMapOf<BlockChainConfig, Repository>()

    /**
     * 如果反复构造Repository会造成底层数据库重复初始化而出错（例如LevelDb），因此不同的BlockChainConfig应该使用不同的Repository实例。
     */
    fun getInstance(config: BlockChainConfig): Repository {
      if (repositoryMap[config] == null) {
        val rep = Repository(config)
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

  val BEST_BLOCK_KEY = "0".toByteArray()

  /**
   * Best block Db.
   */
  private var bestBlockDs: ObjectStore<Block>? = null

  /**
   * Account State的存储类组装。
   */
  fun getAccountStateStore(): PatriciaTrie? {
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
  fun getAccountStore(password: String): ObjectStore<AccountWithKey>? {
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
   * Block的存储类组装。
   */
  fun getBlockStore(): ObjectStore<Block>? {
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
  fun getTransactionStore(): ObjectStore<Transaction>? {
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
  fun getBlockIndexStore(): ObjectStore<List<BlockInfo>>? {
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
  fun getBestBlockStore(): ObjectStore<Block>? {
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
  fun getBalance(address: ByteArray): BigInteger {
    return getAccountState(address)?.balance ?: BigInteger.ZERO
  }

  /**
   * 账户的Nonce+1
   */
  fun increaseNonce(address: ByteArray) {
    val accountState = getOrCreateAccountState(address)
    getAccountStateStore()?.update(address,
        CodecUtil.encodeAccountState(accountState.increaseNonce()))
  }

  /**
   * 增加账户余额。
   */
  fun addBalance(address: ByteArray, amount: BigInteger) {
    val accountState = getOrCreateAccountState(address)
    getAccountStateStore()?.update(address,
        CodecUtil.encodeAccountState(accountState.increaseBalance(amount)))
  }

  fun saveAccount(account: AccountWithKey, password: String = ""): Int {
    val ds = getAccountStore(password)
    val index = ds?.keys()?.size?:0
    val key = index.toString(10).toByteArray()
    ds?.put(key, account)
    return index
  }

  fun getAccount(index: Int, password: String = ""): AccountWithKey? {
    val ds = getAccountStore(password)
    val key = index.toString(10).toByteArray()
    val account = ds?.get(key)
    account?.index = index
    return account
  }

  fun accountNumber(): Int {
    val ds = getAccountStore("")
    val keys = ds?.keys()
    return keys?.size?:0
  }

  fun getBlockInfo(hash: ByteArray): BlockInfo? {
    val block = getBlock(hash)
    if (block != null) {
      val blockInfos = getBlockInfos(block.height)
      return blockInfos?.first { it.hash.contentEquals(block.hash) }
    } else {
      return null
    }
  }

  fun getBlockInfos(height: Long): List<BlockInfo>? {
    return getBlockIndexStore()?.get(CodecUtil.longToByteArray(height))
  }

  fun getBlock(hash: ByteArray): Block? {
    return getBlockStore()?.get(hash)
  }

  fun saveBlock(block: Block) {
    getBlockStore()?.put(block.hash, block)
  }

  fun getBestBlock(): Block? {
    return getBestBlockStore()?.get(BEST_BLOCK_KEY)
  }

  fun updateBestBlock(block: Block) {
    getBestBlockStore()?.put(BEST_BLOCK_KEY, block)
  }

  fun putTransaction(trx: Transaction) {
    getTransactionStore()?.put(trx.hash(), trx)
  }

  fun close() {
    accountDs?.close()
    accountStateDs?.close()
    transactionDs?.close()
    blockDs?.close()
    blockIndexDs?.close()
  }

  /**
   * 新建账户。
   */
  private fun createAccountState(address: ByteArray): AccountState {
    val state = AccountState(BigInteger.ZERO, BigInteger.ZERO)
    getAccountStateStore()?.update(address, CodecUtil.encodeAccountState(state))
    return state
  }

  /**
   * 判断账户状态Account State是不是存在，如果不存在就新建账户。
   */
  private fun getOrCreateAccountState(address: ByteArray): AccountState {
    var ret = getAccountState(address)
    if (ret == null) {
      ret = createAccountState(address)
    }
    return ret
  }

  private fun getAccountState(address: ByteArray) = getAccountStateStore()?.get(
      address)?.let { CodecUtil.decodeAccountState(it) }

}
