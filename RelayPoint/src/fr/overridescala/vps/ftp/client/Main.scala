package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.util.Scanner

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.client.cli.CommandManager
import fr.overridescala.vps.ftp.client.cli.commands.{CreateFileCommand, PingCommand, StressTestCommand, TransferCommand}

object Main {

    private val SERVER_ADDRESS = new InetSocketAddress("161.97.104.230", Constants.PORT)

    print("say 'y' to connect to localhost : ")
    private val scanner = new Scanner(System.in)
    private val isLocalhost = scanner.nextLine().startsWith("y")
    print("choose a identifier : ")
    private val identifier = scanner.nextLine()
    private val address = if (isLocalhost) Constants.LOCALHOST else SERVER_ADDRESS
    private val relayPoint: Relay = new RelayPoint(address, identifier)
    private val commandsManager = new CommandManager(scanner)


    def main(args: Array[String]): Unit = {
        relayPoint.start()
        commandsManager.register("download", TransferCommand.download(relayPoint))
        commandsManager.register("upload", TransferCommand.upload(relayPoint))
        commandsManager.register("crtf", new CreateFileCommand(relayPoint))
        commandsManager.register("ping", new PingCommand(relayPoint))
        commandsManager.register("stress", new StressTestCommand(relayPoint))
        commandsManager.start()
    }

}