package lunar.repl

import lunar.core.BlockChainManager
import lunar.core.Transaction
import lunar.util.CryptoUtil
import org.joda.time.DateTime
import org.spongycastle.util.encoders.Hex
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import java.math.BigInteger

@ShellComponent
class ShellCommands {

  @ShellMethod("Create account.", key= ["account.create"])
  fun accountCreate(password: String): String {
    val account = BlockChainManager.INSTANCE.newAccount(password)
    if (account != null) {
      return "Account[${account.index}]:${Hex.toHexString(
          CryptoUtil.generateAddress(account.publicKey))} created."
    }
    return "Failed to create new account."
  }

  @ShellMethod("Lock account.", key= ["account.lock"])
  fun accountLock(): String {
    if(BlockChainManager.INSTANCE.lockAccount()) {

      return "Account locked."
    } else {
      return "Failed to lock the account."
    }
  }

  @ShellMethod("Unlock account.", key= ["account.unlock"])
  fun accountLoad(index: Int, password: String): String {
    val account = BlockChainManager.INSTANCE.unlockAccount(index, password)
    if (account != null) {
      return "Account[${account.index}]:${Hex.toHexString(
          CryptoUtil.generateAddress(account.publicKey))} loaded."
    }
    return "Failed to unlock account. Make sure your password is correct"
  }

  @ShellMethod("Start miner.", key= ["miner.start"])
  fun minerStart(): String {
    BlockChainManager.INSTANCE.startMining()
    return "Miner started."
  }

  @ShellMethod("Stop miner.", key= ["miner.stop"])
  fun minerStop(): String {
    BlockChainManager.INSTANCE.stopMining()
    return "Miner stopping ..."
  }

  @ShellMethod("Update coinbase address.", key= ["coinbase"])
  fun coinbaseUpdate(coinbase: String): String {
    BlockChainManager.INSTANCE.blockChain.config.setMinerCoinbase(Hex.decode(coinbase))
    return "Coinbase address updated."
  }

  @ShellMethod("Read account balance.", key= ["account.balance"])
  fun accountBalance(address: String): String {
    val balance = BlockChainManager.INSTANCE.blockChain.repository.getBalance(Hex.decode(address))
    return balance.toString()
  }

  @ShellMethod("Account.", key= ["account.transfer"])
  fun accountTransfer(toAddress: String, amount: BigInteger): String {
    val account = BlockChainManager.INSTANCE.currentAccount
    if (account != null) {
      val trx = Transaction(account.address, Hex.decode(toAddress),
          amount, DateTime(), account.publicKey)
      // Alice用私钥签名
      trx.sign(account.privateKey)
      BlockChainManager.INSTANCE.addPendingTransaction(trx)
      return "Transaction $trx created."
    } else {
      return "No account available for transaction. Please unlock the account first."
    }
  }
}