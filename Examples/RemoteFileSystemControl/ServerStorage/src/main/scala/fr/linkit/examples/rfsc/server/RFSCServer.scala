package fr.linkit.examples.rfsc.server

import fr.linkit.api.application.connection.CentralConnection
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}

object RFSCServer {

    private def createConnection(identifier0: String): CentralConnection = {
        val config = new ServerApplicationConfigBuilder {
            val resourcesFolder: String = "C:\\Users\\Maxime\\Desktop\\Dev\\Linkit\\Home"
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier: String = identifier0
                    override val port      : Int    = 48489
                }
            }
        }.buildConfig()
        ServerApplication.launch(config, getClass)
            .findConnection(identifier0)
            .get
    }

    def main(args: Array[String]): Unit = {
        val server = createConnection("RFSCServer")
        print("Server launched !")
    }

}
