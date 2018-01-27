package lunar.miner

import lunar.core.Block

/**
 * 挖矿的结果数据。https://en.bitcoin.it/wiki/Block_hashing_algorithm
 */
data class MineResult(val success: Boolean, val difficulty: Long, val nonce: Int, val block: Block)
