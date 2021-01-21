package fr.`override`.linkit.server

import java.nio.file.Paths

import fr.`override`.linkit.server.config.{AmbiguityStrategy, RelayServerBuilder}


object Main {
    def main(args: Array[String]): Unit = {
        val ideRun = args.contains("--ide-run")
        val relayServer: RelayServer = new RelayServerBuilder {
            relayIDAmbiguityStrategy = AmbiguityStrategy.REJECT_NEW
            enableExtensionsFolderLoad = !ideRun
            extensionsFolder = getExtensionFolderPath
        }
        relayServer.start()

        if (ideRun) {
            /*
            import fr.`override`.linkit.`extension`.cloud.CloudStorageExtension
            import fr.`override`.linkit.`extension`.controller.ControllerExtension
            import fr.`override`.linkit.`extension`.debug.DebugExtension

            relayServer.extensionLoader.loadExtensions(
                classOf[ControllerExtension],
                classOf[CloudStorageExtension],
                classOf[DebugExtension]
            )
             */

        }
    }

    private def getExtensionFolderPath: String = {
        val sourcePath = Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getParent.toString
        System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\ClientSide\\RelayExtensions"
            case "LORDI-N4SO7IERS" => "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\FileTransferer\\ClientSide\\RelayExtensions"
            case _ => sourcePath + "/RelayExtensions/"
        }
    }

}
