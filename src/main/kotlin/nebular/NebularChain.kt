package nebular

import nebular.config.BlockChainConfig
import nebular.core.BlockChain
import nebular.core.BlockChainManager
import nebular.network.server.PeerServer
import nebular.repl.NebularShell
import org.apache.commons.cli.*
import org.springframework.boot.SpringApplication
import java.io.File

lateinit var CHAIN_MANAGER: BlockChainManager

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
  CHAIN_MANAGER = BlockChainManager(blockChain)

  val terminal = cmd.hasOption('t')
  if (terminal) {
    //startTerminal(manager)
    val context = SpringApplication.run(NebularShell::class.java)
  } else {
    var server: PeerServer
    server = PeerServer(CHAIN_MANAGER)
    server.start()

    CHAIN_MANAGER.startPeerDiscovery()
    println("NebularChain started.")
  }
}
