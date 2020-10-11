package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.Scanner

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.client.auto.{AutoUploader, AutomationManager}
import fr.overridescala.vps.ftp.client.cli.{CommandExecutor, CommandManager}
import fr.overridescala.vps.ftp.client.cli.commands.{CreateFileCommand, DeleteFileCommand, ExecuteUnknownTaskCommand, ListenDirCommand, PingCommand, StressTestCommand, TransferCommand}

object Main {

    private val SERVER_ADDRESS = new InetSocketAddress("161.97.104.230", Constants.PORT)
    private val LOCALHOST = new InetSocketAddress("localhost", Constants.PORT)

    print("say 'y' to connect to localhost : ")
    private val scanner = new Scanner(System.in)
    private val isLocalhost = scanner.nextLine().startsWith("y")
    print("choose a identifier : ")
    private val identifier = scanner.nextLine()
    private val address = if (isLocalhost) LOCALHOST else SERVER_ADDRESS
    private val relayPoint: Relay = new RelayPoint(address, identifier)

    private val commandsManager = new CommandManager(scanner)
    private val automationManager = new AutomationManager()


    def main(args: Array[String]): Unit = {
        relayPoint.start()

        commandsManager.register("download", TransferCommand.download(relayPoint))
        commandsManager.register("upload", TransferCommand.upload(relayPoint))
        commandsManager.register("crtf", new CreateFileCommand(relayPoint))
        commandsManager.register("ping", new PingCommand(relayPoint))
        commandsManager.register("stress", new StressTestCommand(relayPoint))
        commandsManager.register("exec", new ExecuteUnknownTaskCommand(relayPoint))
        commandsManager.register("delete", new DeleteFileCommand(relayPoint))
        commandsManager.register("listen", new ListenDirCommand(relayPoint, automationManager))
        commandsManager.start()
        automationManager.start()
        args.foreach(commandsManager.injectLine)
    }

}