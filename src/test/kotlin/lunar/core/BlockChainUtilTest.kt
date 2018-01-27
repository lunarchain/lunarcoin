package lunar.core

import lunar.util.BlockChainUtil
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.junit.Test
import kotlin.test.assertEquals

class BlockChainUtilTest {

  @Test
  fun calculateDifficultyTest() {
    val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val time = DateTime.parse("2017-08-28 11:24:23", format).millis/1000
    val parentTime = DateTime.parse("2017-08-28 11:25:43", format).millis/1000
    val difficulty = BlockChainUtil.calculateDifficulty(time, parentTime, 4212371,  2117963098883076)
    assertEquals(difficulty, 2111823478825220)
  }
}