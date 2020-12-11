package fr.overridescala.linkkit.server

import fr.overridescala.linkkit.api.Relay
import fr.overridescala.linkkit.server.config.{AmbiguityStrategy, RelayServerBuilder}

object Main {
    def main(args: Array[String]): Unit = {
        val relayServer: Relay = new RelayServerBuilder {
            relayIDAmbiguityStrategy = AmbiguityStrategy.REJECT_NEW

            extensionsFolder = getExtensionFolderPath
        }
        relayServer.start()
    }

    private def getExtensionFolderPath: String = {
        System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\RelayExtensions"
            case "LORDI-N4SO7IERS" => "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\FileTransferer\\ClientSide\\RelayExtensions"
            case _ => "/RelayExtensions/"
        }
    }

}
