package lunar.serialization

import lunar.core.*
import lunar.storage.BlockInfo
import lunar.util.CodecUtil
import lunar.util.CryptoUtil

/**
 * 序列化/反序列化接口。
 */
interface Serializer<T, S> {

  /**
   * Converts T ==> S
   * Should correctly handle null parameter
   */
  fun serialize(obj: T): S

  /**
   * Converts S ==> T
   * Should correctly handle null parameter
   */
  fun deserialize(s: S): T?
}

class AccountStateSerialize : Serializer<AccountState, ByteArray> {
  override fun deserialize(s: ByteArray): AccountState? {
    return CodecUtil.decodeAccountState(s)
  }

  override fun serialize(obj: AccountState): ByteArray {
    return CodecUtil.encodeAccountState(obj)
  }

}

class AccountSerialize(val password:String) : Serializer<AccountWithKey, ByteArray> {
  override fun deserialize(s: ByteArray): AccountWithKey? {
    try {
      val privateKey = CryptoUtil.decryptPrivateKey(s, password)
      val publicKey = CryptoUtil.generatePublicKey(privateKey)
      return if (publicKey != null) {
        AccountWithKey(publicKey, privateKey)
      } else {
        null
      }
    } catch (e: Exception) {
      return null
    }
  }

  override fun serialize(obj: AccountWithKey): ByteArray {
    return CryptoUtil.encryptPrivateKey(obj.privateKey, password)
  }

}

class BlockSerialize : Serializer<Block, ByteArray> {
  override fun deserialize(s: ByteArray): Block? {
    return CodecUtil.decodeBlock(s)
  }

  override fun serialize(obj: Block): ByteArray {
    return CodecUtil.encodeBlock(obj)
  }

}

class TransactionSerialize : Serializer<Transaction, ByteArray> {
  override fun deserialize(s: ByteArray): Transaction? {
    return CodecUtil.decodeTransaction(s)
  }

  override fun serialize(obj: Transaction): ByteArray {
    return CodecUtil.encodeTransaction(obj)
  }

}

class BlockInfosSerialize : Serializer<List<BlockInfo>, ByteArray> {
  override fun deserialize(s: ByteArray): List<BlockInfo>? {
    return CodecUtil.decodeBlockInfos(s)
  }

  override fun serialize(obj: List<BlockInfo>): ByteArray {
    return CodecUtil.encodeBlockInfos(obj)
  }

}
