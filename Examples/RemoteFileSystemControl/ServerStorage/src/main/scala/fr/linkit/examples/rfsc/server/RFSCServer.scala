package fr.linkit.examples.rfsc.server

import fr.linkit.api.application.connection.CentralConnection
import fr.linkit.api.gnom.cache.sync.contract.Contract
import fr.linkit.api.gnom.cache.sync.contract.behavior.ObjectsProperty
import fr.linkit.api.gnom.network.Engine
import fr.linkit.api.internal.concurrency.pool.WorkerPools
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}

import java.nio.file.{Files, Path}
import scala.collection.mutable

object RFSCServer {
    
    private val clientsHomes        = new mutable.HashMap[Engine, Path]()
    private val watchServiceWorkers = WorkerPools.newClosedPool("WatchServicesWorkers")
    
    def main(args: Array[String]): Unit = {
        val server = createConnection("RFSCServer")
        println("Server launched !")
        val network = server.network
        network.onNewEngine { client => {
            val path = Path.of(s"C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\StaticsFTPTests\\Server\\${client.identifier}\\")
            Files.createDirectories(path)
            clientsHomes.put(client, path)
            watchServiceWorkers.setThreadCount(clientsHomes.size)
        }
        }
        val prop      = ObjectsProperty("fs")(Map("homes" -> clientsHomes, "watchServiceWorkers" -> watchServiceWorkers))
        val contracts = Contract("FSControl", prop)
        network.newStaticAccess(1, contracts)
    }
    
    private def createConnection(identifier0: String): CentralConnection = {
        val config = new ServerApplicationConfigBuilder {
            val resourcesFolder: String = "C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home"
            logfilename = Some("rsfc server")
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
