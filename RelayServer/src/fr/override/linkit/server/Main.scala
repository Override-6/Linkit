package fr.`override`.linkit.server

import java.nio.file.Paths

import fr.`override`.linkit.server.config.{AmbiguityStrategy, RelayServerBuilder}
import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.server.config.AmbiguityStrategy

object Main {
    def main(args: Array[String]): Unit = {
        val relayServer: Relay = new RelayServerBuilder {
            relayIDAmbiguityStrategy = AmbiguityStrategy.REJECT_NEW

            extensionsFolder = getExtensionFolderPath
        }
        relayServer.start()
    }

    private def getExtensionFolderPath: String = {
        val sourcePath = Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getParent.toString
        System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\RelayExtensions"
            case "LORDI-N4SO7IERS" => "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\FileTransferer\\ClientSide\\RelayExtensions"
            case _ => sourcePath + "/RelayExtensions/"
        }
    }

}
