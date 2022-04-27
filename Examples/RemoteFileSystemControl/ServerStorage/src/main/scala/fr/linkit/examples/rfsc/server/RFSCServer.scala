package fr.linkit.examples.rfsc.server

import fr.linkit.api.application.connection.CentralConnection
import fr.linkit.api.gnom.network.Engine
import fr.linkit.engine.internal.language.bhv.{Contract, ObjectsProperty}
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}

import java.nio.file.Path
import scala.collection.mutable

object RFSCServer {

    private val clientsHomes = new mutable.HashMap[Engine, Path]()

    def main(args: Array[String]): Unit = {
        val server = createConnection("RFSCServer")
        println("Server launched !")
        val network = server.network
        network.onNewEngine { client =>
            clientsHomes.put(client, Path.of(s"D:\\Users\\maxim\\Desktop\\Dev\\Perso\\Linkit\\StaticsFTPTest\\Client\\${client.identifier}\\"))
        }
        val contracts = Contract("FSControl", network.connection.getApp, ObjectsProperty(Map("homes" -> clientsHomes)))
        network.newStaticAccess(1, contracts)
    }

    private def createConnection(identifier0: String): CentralConnection = {
        val config = new ServerApplicationConfigBuilder {
            val resourcesFolder: String = "C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home"
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

}
