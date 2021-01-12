package fr.`override`.linkit.client

import java.net.InetSocketAddress
import java.nio.file.Paths
import java.util.Scanner

import fr.`override`.linkit.client.config.RelayPointBuilder

object Main {

    private val PORT = 48484
    private val SERVER_ADDRESS = new InetSocketAddress("192.168.1.19", PORT)
    private val LOCALHOST = new InetSocketAddress("localhost", PORT)

    print("say 'y' to connect to localhost : ")
    private val scanner = new Scanner(System.in)
    private val isLocalhost = scanner.nextLine().startsWith("y")
    print("choose an identifier : ")
    private val identifier = scanner.nextLine()
    private val address = if (isLocalhost) LOCALHOST else SERVER_ADDRESS

    /**
     * @param args "--ide-run", used to determine if the application is running into IntelliJ or from an external context
     *             "--no-tasks", used to decide if the relay should not load any tasks from his RelayExtensions.
     * */
    def main(args: Array[String]): Unit = {
        val ideRun = args.contains("--ide-run")
        val loadExtensions = !(args.contains("--no-ext") || ideRun)

        val relayPoint: RelayPoint = new RelayPointBuilder {
            enableExtensionsFolderLoad = loadExtensions
            extensionsFolder = getExtensionFolderPath

            override var serverAddress: InetSocketAddress = address
            override var identifier: String = Main.this.identifier
        }
        relayPoint.start()

        if (ideRun && relayPoint.isOpen) {

            import fr.`override`.linkit.`extension`.cloud.CloudStorageExtension
            import fr.`override`.linkit.`extension`.controller.ControllerExtension
            import fr.`override`.linkit.`extension`.debug.DebugExtension

            val loader = relayPoint.extensionLoader
            loader.loadExtensions(
                classOf[ControllerExtension],
                classOf[CloudStorageExtension],
                classOf[DebugExtension]
            )
        }
    }

    private def getExtensionFolderPath: String = {
        lazy val sourcePath = Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getParent.toString
        System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\RelayExtensions"
            case "LORDI-N4SO7IERS" => "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\FileTransferer\\ClientSide\\RelayExtensions"
            case _ => sourcePath + "/RelayExtensions/"
        }
    }

}
