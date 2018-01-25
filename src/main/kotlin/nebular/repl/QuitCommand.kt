package nebular.repl

import nebular.CHAIN_MANAGER
import org.springframework.shell.ExitRequest
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.commands.Quit


@ShellComponent
open class QuitCommand : Quit.Command {

  @ShellMethod("Quit.")
  fun quit() {
    println("Quit NebularChain...")
    CHAIN_MANAGER.stop()

    throw ExitRequest()
  }
}
