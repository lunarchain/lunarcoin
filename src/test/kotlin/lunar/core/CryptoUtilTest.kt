package lunar.core

import lunar.util.CryptoUtil
import org.junit.Assert
import org.junit.Test


class CryptoUtilTest {

  @Test
  fun privateKeyEncryptDecryptTest() {
    val kp = CryptoUtil.generateKeyPair()
    val privateKey = kp.private
    val password = "pleasechangeit"

    val encrypted = CryptoUtil.encryptPrivateKey(privateKey, password)
    val decrypted = CryptoUtil.decryptPrivateKey(encrypted, password)

    Assert.assertArrayEquals(privateKey.encoded, decrypted.encoded)
  }

}
