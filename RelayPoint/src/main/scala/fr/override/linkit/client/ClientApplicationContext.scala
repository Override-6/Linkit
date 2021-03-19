package fr.`override`.linkit.client

import java.net.InetSocketAddress

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.local.ApplicationContext
import fr.`override`.linkit.api.local.plugin.PluginManager
import fr.`override`.linkit.api.local.system.config.ApplicationConfiguration
import fr.`override`.linkit.client.config.ClientApplicationConfiguration
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool
import fr.`override`.linkit.core.local.plugin.LinkitPluginManager

import scala.collection.mutable

class ClientApplicationContext(override val configuration: ClientApplicationConfiguration) extends ApplicationContext {

    private val workerPool = new BusyWorkerPool(configuration.nWorkerThreadFunction(0))
    private val connections = mutable.HashMap.empty[String, ConnectionContext]

    override val pluginManager: PluginManager = new LinkitPluginManager(configuration.fsAdapter)

    override def getConnection(identifier: String): ConnectionContext = {
        val opt = connections.get(identifier)
        if (opt.isEmpty)
            throw new IllegalArgumentException(s"No connection found for identifier $identifier")
        opt.get
    }

    def newConnection(port: Int, address: InetSocketAddress): ConnectionContext = {
        val socket = configuration.socketFactory(port, address)

    }

    override def runLater(task: => Unit): Unit = workerPool.runLater(task)
}
