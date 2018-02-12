package lunar.core

import lunar.config.Constants.COINBASE_SENDER_ADDRESS
import lunar.util.CodecUtil
import lunar.util.CryptoUtil
import org.joda.time.DateTime
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*

/**
 * 交易记录类：记录了发送方(sender)向接受方(receiver)的转账记录，包括金额(amount)、时间戳(time)、发送方的公钥和签名。
 * 为简化模型，没有加入费用(fee)。
 */
class Transaction(val senderAddress: ByteArray, val receiverAddress: ByteArray, val amount: BigInteger,
    val time: DateTime, val publicKey: PublicKey, var signature: ByteArray = ByteArray(0)) {

  /**
   * 交易合法性验证。目前只验证签名长度和签名合法性。
   */
  val isValid: Boolean
    get() = (isCoinbaseTransaction() ||
        (signature.isNotEmpty() && CryptoUtil.verifyTransactionSignature(this, signature)))

  /**
   * 是否为Coinbase Transaction。
   */
  fun isCoinbaseTransaction() = senderAddress.contentEquals(COINBASE_SENDER_ADDRESS)

  /**
   * 用发送方的私钥进行签名。
   */
  fun sign(privateKey: PrivateKey): ByteArray {
    signature = CryptoUtil.signTransaction(this, privateKey)
    return signature
  }

  fun hash(): ByteArray {
    return CryptoUtil.hashTransaction(this)
  }

  fun encode(): ByteArray {
    return CodecUtil.encodeTransaction(this)
  }

  override fun equals(other: Any?): Boolean {
    if (other is Transaction) {
      if (!Arrays.equals(this.senderAddress, other.senderAddress)) return false
      if (!Arrays.equals(this.receiverAddress, other.receiverAddress)) return false
      if (this.amount != other.amount) return false
      if (this.time != other.time) return false
      if (this.publicKey != other.publicKey) return false

      return true
    } else {
      return false
    }
  }

  override fun toString(): String {
    return "Trx Hash:${Hex.toHexString(hash())} Amount:$amount From:${Hex.toHexString(senderAddress)} To:${Hex.toHexString(receiverAddress)} in $time"
  }
}
