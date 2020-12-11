package fr.overridescala.linkkit.client

import java.net.InetSocketAddress
import java.nio.file.{Path, Paths}
import java.util.Scanner

//import fr.overridescala.linkkit.`extension`.cloud.CloudStorageExtension
import fr.overridescala.linkkit.`extension`.controller.ControllerExtension
//import fr.overridescala.linkkit.`extension`.debug.DebugExtension
import fr.overridescala.linkkit.client.config.RelayPointBuilder

object Main {

    private val PORT = 48484
    private val SERVER_ADDRESS = new InetSocketAddress("161.97.104.230", PORT)
    private val LOCALHOST = new InetSocketAddress("localhost", PORT)

    print("say 'y' to connect to localhost : ")
    private val scanner = new Scanner(System.in)
    private val isLocalhost = scanner.nextLine().startsWith("y")
    print("choose a identifier : ")
    private val identifier = scanner.nextLine()
    private val address = if (isLocalhost) LOCALHOST else SERVER_ADDRESS

    /**
     * @param args "--local-run", used to determine if the application is run into IntelliJ or from an external context
     *             "--no-tasks", used to decide if the relay should not load any tasks from his RelayExtensions.
     * */
    def main(args: Array[String]): Unit = {
        val localRun = args.contains("--local-run")
        val loadExtensions = !(args.contains("--no-ext") || localRun)

        val relayPoint = new RelayPointBuilder {
            enableExtensionsFolderLoad = loadExtensions
            extensionsFolder = getExtensionFolderPath

            override var serverAddress: InetSocketAddress = address
            override var identifier: String = Main.this.identifier
        }
        relayPoint.start()

        if (localRun) {
            val loader = relayPoint.extensionLoader
            loader.loadExtension(classOf[ControllerExtension])
            //loader.loadExtension(classOf[DebugExtension])
            //loader.loadExtension(classOf[CloudStorageExtension])
        }
    }

    private def getExtensionFolderPath: String = {
        System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\RelayExtensions"
            case "LORDI-N4SO7IERS" => "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\FileTransferer\\ClientSide\\RelayExtensions"
            case _ => "/RelayExtensions/"
        }
    }

}