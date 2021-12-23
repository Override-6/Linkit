package fr.linkit.examples.ssc.client

import fr.linkit.api.application.connection.ExternalConnection
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.schematic.ScalaClientAppSchematic
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfigBuilder}
import fr.linkit.engine.gnom.cache.sync.DefaultSynchronizedObjectCache
import fr.linkit.examples.ssc.api.UserAccountContainer

import java.net.InetSocketAddress
import scala.io.StdIn

object ClientSideMain {

    private final val Address = new InetSocketAddress("localhost", 48481)

    def main(args: Array[String]): Unit = {
        val accounts = connectToAccounts()
        println("Successfully connected !")
        val handler = new UserInputHandler(accounts)
        do {
            val inputs = StdIn.readLine().split("\\s")
            handler.performCommand(inputs.head, inputs.drop(1))
        } while (true)
    }

    private def connectToAccounts(): UserAccountContainer = {
        println("Choose the user name you want to control")
        print("> ")
        val username = StdIn.readLine()
        println(s"Opening connection as '$username'.")
        val connection = launchApp(username)
        val global     = connection.network.globalCache
        val cache      = global.attachToCache(51, DefaultSynchronizedObjectCache[UserAccountContainer]())
        cache.findObject(0).getOrElse(throw new NoSuchElementException("could not find accounts container DAO"))
    }

    private def launchApp(identifier0: String): ExternalConnection = {
        val config = new ClientApplicationConfigBuilder {
            val resourcesFolder: String = "C:\\Users\\Maxime\\Desktop\\Dev\\Linkit\\Home"
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
