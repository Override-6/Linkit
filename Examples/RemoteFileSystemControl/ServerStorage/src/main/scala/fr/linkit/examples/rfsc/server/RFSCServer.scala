package fr.linkit.examples.rfsc.server

import fr.linkit.api.application.connection.CentralConnection
import fr.linkit.api.gnom.network.Engine
import fr.linkit.engine.internal.language.bhv.{Contract, ObjectsProperty}
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}

import java.nio.file.{Files, Path}
import scala.collection.mutable

object RFSCServer {

    private val clientsHomes = new mutable.HashMap[Engine, Path]()

    def main(args: Array[String]): Unit = {
        val server = createConnection("RFSCServer")
        println("Server launched !")
        val network = server.network
        network.onNewEngine { client => {
            val path = Path.of(s"C:\\Users\\Maxime\\Desktop\\Dev\\Linkit\\StaticsFTPTests\\Server\\${client.identifier}\\")
            if (Files.notExists(path))
                Files.createDirectories(path)
            clientsHomes.put(client, path)
            print(clientsHomes, System.identityHashCode(clientsHomes))
        }
        }
        val prop = ObjectsProperty(Map("homes" -> clientsHomes))
        val contracts = Contract("FSControl", network.connection.getApp, prop)
        network.newStaticAccess(1, contracts)
    }

    private def createConnection(identifier0: String): CentralConnection = {
        val config = new ServerApplicationConfigBuilder {
            val resourcesFolder: String = "C:\\Users\\Maxime\\Desktop\\Dev\\Linkit\\Home"
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier = identifier0
                    override val port       = 48489
                    defaultPersistenceConfigScript = Some(getClass.getResource("/FS_persistence.sc"))
                }
            }
        }.buildConfig()
        ServerApplication.launch(config, getClass)
            .findConnection(identifier0)
            .get
    }

}
