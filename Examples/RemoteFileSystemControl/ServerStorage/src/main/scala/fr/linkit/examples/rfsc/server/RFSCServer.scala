package fr.linkit.examples.rfsc.server

import fr.linkit.api.application.connection.CentralConnection
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor
import fr.linkit.engine.internal.language.bhv.Contract
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.schematic.ScalaServerAppSchematic
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfigBuilder}

import java.nio.file.Path
import scala.collection.mutable

object RFSCServer {

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

    def main(args: Array[String]): Unit = {
        val server = createConnection("RFSCServer")
        print("Server launched !")
        val network    = server.network
        val contracts  = Contract("FSControl")(network)
        val properties = network.globalCache
                .attachToCache(40, DefaultSynchronizedObjectCache[mutable.HashMap[String, String]])
                .syncObject(0, Constructor[mutable.HashMap[String, String]](), contracts)
        val statics    = network.newStaticAccess(1, contracts)
        val wtf = statics[Path].of[Path]("C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\Home\\CompilationCenter")
        println(wtf)
    }

}
