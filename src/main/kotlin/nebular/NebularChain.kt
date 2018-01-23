package nebular

import nebular.config.BlockChainConfig
import nebular.core.BlockChain
import nebular.core.BlockChainManager
import nebular.network.server.PeerServer
import nebular.repl.Command
import nebular.util.CryptoUtil
import org.apache.commons.cli.*
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import org.spongycastle.util.encoders.Hex
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

fun main(args: Array<String>) {
  println("NebularChain starting ......")
  val options = Options()

  val configOption = Option("c", "config", true, "config file path")
  configOption.isRequired = false
  options.addOption(configOption)

  val terminalOption = Option("t", "terminal", false, "run nebular interactive terminal")
  terminalOption.isRequired = false
  options.addOption(terminalOption)

  val parser = DefaultParser()
  val formatter = HelpFormatter()
  val cmd: CommandLine

  try {
    cmd = parser.parse(options, args)
  } catch (e: ParseException) {
    println(e.message)
    formatter.printHelp("NebularChain", options)

    System.exit(1)
    return
  }

  val configFilePath = cmd.getOptionValue("config") ?: "conf/application.conf"
  val configFile = File(configFilePath)
  val blockChain = if (configFile.exists()) BlockChain(BlockChainConfig.default()) else BlockChain(
      BlockChainConfig(configFile))
  val manager = BlockChainManager(blockChain)
  val chain = NebularChain()
  chain.manager = manager
  chain.init()
  NebularChain.instance = chain

  val terminal = cmd.hasOption('t')
  if (terminal) {
    startTerminal(manager)
  } else {
    chain.start()
    println("NebularChain started.")
  }
}

fun startTerminal(manager: BlockChainManager) {
  // Suppress log messages from JLine.
  val logger = Logger.getLogger("org.jline");
  logger.level = Level.SEVERE

  val terminal = TerminalBuilder.terminal()
  terminal.echo(false)
  val lineReader = LineReaderBuilder.builder()
      .terminal(terminal)
      .build()
  terminalloop@ while (true) {
    val input = lineReader.readLine("> ")
    val command = Command(input)
    when (command.name) {
      "quit" -> break@terminalloop
      "exit" -> break@terminalloop
      "account.new" -> {
        val password = if (command.args.size > 0) command.getStringArgument(0) else ""
        val account = manager.newAccount(password)
        if (account != null) {
          println("Account[${account.index}]:${Hex.toHexString(CryptoUtil.generateAddress(account.publicKey))} created.")
        } else {
          println("Failed to create new account.")
        }
      }
      "account.load" -> {
        val index = if (command.args.size > 0) command.getIntArgument(0) else 0
        val password = if (command.args.size > 1) command.getStringArgument(1) else ""

        val account = manager.getAccount(index, password)
        if (account != null) {
          println("Account[${account.index}]:${Hex.toHexString(CryptoUtil.generateAddress(account.publicKey))} loaded.")
        } else {
          println("Failed to load account.")
        }
      }
    }
  }
}

class NebularChain {
  companion object {
    var instance: NebularChain? = null
  }

  lateinit var manager: BlockChainManager
  lateinit var server: PeerServer

  fun init() {
  }

  fun start() {
    server = PeerServer(manager)
    server.start()

    manager.startPeerDiscovery()
//        manager.startMining()
  }
}
