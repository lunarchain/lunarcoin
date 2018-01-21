package nebular

import nebular.config.BlockChainConfig
import nebular.core.BlockChain
import nebular.core.BlockChainManager
import nebular.network.server.PeerServer
import org.apache.commons.cli.*
import java.io.File


fun main(args: Array<String>) {
  println("NebularChain starting ......")
  val options = Options()

  val configOption = Option("c", "config", true, "config file path")
  configOption.isRequired = false
  options.addOption(configOption)

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

  var mbc: NebularChain
  val configFilePath = cmd.getOptionValue("config") ?: "conf/application.conf"
  val configFile = File(configFilePath)
  if (!configFile.exists()) {
    mbc = NebularChain()
  } else {
    mbc = NebularChain(BlockChainConfig(configFile))
  }
  mbc.init()
  mbc.start()
  println("NebularChain started.")
}

class NebularChain(val config: BlockChainConfig = BlockChainConfig.default()) {

  lateinit var server: PeerServer

  val blockChain = BlockChain(config)

  fun init() {
  }

  fun start() {
    val manager = BlockChainManager(blockChain)

    server = PeerServer(manager)
    server.start()

    manager.startPeerDiscovery()
//        manager.startMining()
  }
}
