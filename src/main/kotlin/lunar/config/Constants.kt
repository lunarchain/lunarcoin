package lunar.config

import org.spongycastle.util.encoders.Hex
import java.math.BigInteger

object Constants {
  val BLOCK_REWARD = BigInteger.valueOf(5000000000000000000)
  val DEFAULT_DIFFICULTY = 0x1d80ffffL // 比特币的最小(初始)难度为0x1d00ffff，为测试方便我们降低难度为0x1e00ffff
  val COINBASE_SENDER_ADDRESS = Hex.decode("0000000000000000000000000000000000000000")
}