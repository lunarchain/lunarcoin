package io.lunarchain.lunarcoin

import io.lunarchain.lunarcoin.config.BlockChainConfig
import io.lunarchain.lunarcoin.core.BlockChain
import io.lunarchain.lunarcoin.core.BlockChainManager
import io.lunarchain.lunarcoin.network.server.PeerServer
import io.lunarchain.lunarcoin.repl.LunarCoinShell
import io.lunarchain.lunarcoin.storage.ServerRepository
import org.apache.commons.cli.*
import org.springframework.boot.SpringApplication
import java.io.File

fun main(args: Array<String>) {
    println("LunarCoin starting ......")
    val options = Options()

    val configOption = Option("c", "config", true, "config file path")
    configOption.isRequired = false
    options.addOption(configOption)

    val terminalOption = Option("t", "terminal", false, "run lunar interactive terminal")
    terminalOption.isRequired = false
    options.addOption(terminalOption)

    val parser = DefaultParser()
    val formatter = HelpFormatter()
    val cmd: CommandLine

    try {
        cmd = parser.parse(options, args)
    } catch (e: ParseException) {
        println(e.message)
        formatter.printHelp("LunarCoin", options)

        System.exit(1)
        return
    }

    val configFilePath = cmd.getOptionValue("config") ?: "conf/application.conf"
    val configFile = File(configFilePath)
    val blockChainConfig = if (!configFile.exists()) BlockChainConfig.default() else BlockChainConfig(configFile)
    val blockChain = BlockChain(blockChainConfig, ServerRepository.getInstance(blockChainConfig))
    val manager = BlockChainManager(blockChain)

    val terminal = cmd.hasOption('t')
    if (terminal) {
        SpringApplication.run(LunarCoinShell::class.java)
    } else {
        var server: PeerServer
        server = PeerServer(manager)
        server.start()

        manager.startPeerDiscovery()
        println("LunarCoin started.")
    }
}
