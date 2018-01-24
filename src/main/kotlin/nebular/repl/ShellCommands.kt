package nebular.repl

import nebular.CHAIN_MANAGER
import nebular.util.CryptoUtil
import org.spongycastle.util.encoders.Hex
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod


@ShellComponent
class ShellCommands {

  @ShellMethod("Create account.", key= ["account.create"])
  fun accountCreate(password: String): String {
    val account = CHAIN_MANAGER.newAccount(password)
    if (account != null) {
      return "Account[${account.index}]:${Hex.toHexString(
          CryptoUtil.generateAddress(account.publicKey))} created."
    }
    return "Failed to create new account."
  }

  @ShellMethod("Load account.", key= ["account.load"])
  fun accountLoad(index: Int, password: String): String {
    val account = CHAIN_MANAGER.getAccount(index, password)
    if (account != null) {
      return "Account[${account.index}]:${Hex.toHexString(
          CryptoUtil.generateAddress(account.publicKey))} loaded."
    }
    return "Failed to load account."
  }

  @ShellMethod("Start miner.", key= ["miner.start"])
  fun minerStart(): String {
    CHAIN_MANAGER.startMining()
    return "Miner started."
  }

  @ShellMethod("Stop miner.", key= ["miner.stop"])
  fun minerStop(): String {
    CHAIN_MANAGER.stopMining()
    return "Miner stopping ..."
  }
}