package lunar.repl

import lunar.core.BlockChainManager
import org.springframework.shell.ExitRequest
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.commands.Quit


@ShellComponent
open class QuitCommand : Quit.Command {

  @ShellMethod("Quit.")
  fun quit() {
    println("Quit LunarCoin...")
    BlockChainManager.INSTANCE.stop()

    throw ExitRequest()
  }
}
