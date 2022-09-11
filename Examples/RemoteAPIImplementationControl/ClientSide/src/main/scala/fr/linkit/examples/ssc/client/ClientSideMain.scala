package fr.linkit.examples.ssc.client

import fr.linkit.api.application.connection.{ConnectionContext, ExternalConnection}
import fr.linkit.api.gnom.cache.sync.ConnectedObjectCache
import fr.linkit.api.internal.concurrency.pool.WorkerPools
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}
import fr.linkit.examples.ssc.api.UserAccountContainer

import java.net.InetSocketAddress
import scala.io.StdIn

object ClientSideMain {

    private final val Address = new InetSocketAddress("localhost", 48481)

    def main(args: Array[String]): Unit = {

        val pool = WorkerPools.newHiringPool("main")
        pool.hireCurrentThread()
        start()
    }

    private def start(): Unit = {
        println("Choose the user name you want to control")
        print("> ")
        val username = StdIn.readLine()
        println(s"Opening connection as '$username'.")
        val connection = launchApp(username)
        val accounts = connectToAccounts(connection)
        println("Successfully connected !")
        val handler = new UserInputHandler(accounts)
        do {
            val inputs = StdIn.readLine().split("\\s")
            handler.performCommand(inputs.head, inputs.drop(1))
        } while (true)
    }

    private def connectToAccounts(connection: ConnectionContext): UserAccountContainer = {
        val global     = connection.network.globalCaches
        val cache      = global.attachToCache(51, ConnectedObjectCache[UserAccountContainer])
        cache.findObject(0).getOrElse(throw new NoSuchElementException("could not find accounts container"))
    }

    private def launchApp(identifier0: String): ExternalConnection = {
        val config = new ClientApplicationConfigBuilder {
            val resourcesFolder: String = System.getenv("LinkitHome")
            loadSchematic = new ScalaClientAppSchematic {
                clients += new ClientConnectionConfigBuilder {
                    override val identifier   : String            = identifier0
                    override val remoteAddress: InetSocketAddress = Address
                }
            }
        }
        ClientApplication.launch(config, getClass, classOf[UserAccountContainer])
            .findConnection(identifier0)
            .get
    }

}
