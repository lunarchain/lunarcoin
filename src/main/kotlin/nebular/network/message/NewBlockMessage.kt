package nebular.network.message

import nebular.core.Block
import nebular.util.CodecUtil

class NewBlockMessage(val block: Block) : Message {
  override fun code(): Byte {
    return MessageCodes.NEW_BLOCK.code
  }

  override fun encode(): ByteArray {

    return CodecUtil.encodeBlock(block)
  }

  companion object {
    fun decode(bytes: ByteArray): NewBlockMessage? {
      val block = CodecUtil.decodeBlock(bytes)
      if (block != null) {
        return NewBlockMessage(block)
      } else {
        return null
      }
    }
  }
}
