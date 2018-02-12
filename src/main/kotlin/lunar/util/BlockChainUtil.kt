package lunar.util

import lunar.config.Constants.DEFAULT_DIFFICULTY
import lunar.core.Block
import lunar.core.BlockChainManager
import org.spongycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.math.max

object BlockChainUtil {
  /**
   * 区块合法性验证，再次执行哈希算法并与target值做对比。
   */
  fun validateBlock(block: Block): Boolean {
    val headerBuffer = ByteBuffer.allocate(4 + 32 + 32 + 4 + 8 + 4)
    val ver = block.version
    val parentHash = block.parentHash
    val merkleRoot = block.trxTrieRoot
    val time = (block.time.millis / 1000).toInt() // Current timestamp as seconds since 1970-01-01T00:00 UTC
    val difficulty = BlockChainManager.INSTANCE.calculateBlockDifficulty(block) // difficulty
    val nonce = block.nonce

    val exp = (difficulty shr 24).toInt()
    val mant = difficulty and 0xffffff
    val target = BigInteger.valueOf(mant).multiply(
        BigInteger.valueOf(2).pow(8 * (exp - 3)))
    val targetStr = "%064x".format(target)

    headerBuffer.put(ByteBuffer.allocate(4).putInt(ver).array()) // version
    headerBuffer.put(parentHash) // parentHash
    headerBuffer.put(merkleRoot) // trxTrieRoot
    headerBuffer.put(ByteBuffer.allocate(4).putInt(time).array()) // time
    headerBuffer.put(
        ByteBuffer.allocate(8).putLong(difficulty).array()) // difficulty(current difficulty)
    headerBuffer.put(ByteBuffer.allocate(4).putInt(nonce).array()) // nonce

    val header = headerBuffer.array()
    val hit = Hex.toHexString(CryptoUtil.sha256(CryptoUtil.sha256(header)))

    return hit < targetStr
  }

  fun calculateDifficulty(time: Long, parentTime: Long, parentHeight: Long,
      parentDifficulty: Long): Long {
    // https://github.com/ethereum/EIPs/blob/master/EIPS/eip-2.mediawiki
    // algorithm:
    // diff = (parent_diff +
    //         (parent_diff / 2048 * max(1 - (block_timestamp - parent_timestamp) // 10, -99))
    //        ) + 2^(periodCount - 2)

    val x = 1 - (parentTime - time) / 10
    val m = max(x, -99)

    //Sub-formula B - The difficulty bomb part, which increases the difficulty exponentially every 100,000 blocks.
    val bomb = Math.pow(2.toDouble(), (((parentHeight + 1) / 100000) - 2).toDouble()).toLong()

    val difficulty = parentDifficulty + parentDifficulty / 2048 * m + bomb
    return if (difficulty > DEFAULT_DIFFICULTY) DEFAULT_DIFFICULTY else difficulty
  }
}