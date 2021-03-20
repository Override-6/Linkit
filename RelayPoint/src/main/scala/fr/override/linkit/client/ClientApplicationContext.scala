package fr.`override`.linkit.client

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.local.ApplicationContext
import fr.`override`.linkit.api.local.plugin.PluginManager
import fr.`override`.linkit.api.local.system.config.ApplicationConfiguration
import fr.`override`.linkit.client.config.ClientConnectionConfiguration
import fr.`override`.linkit.core.connection.packet.traffic.DynamicSocket
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool
import fr.`override`.linkit.core.local.plugin.LinkitPluginManager
import fr.`override`.linkit.core.local.system.ContextLogger

import scala.collection.mutable

class ClientApplicationContext(override val configuration: ApplicationConfiguration) extends ApplicationContext {

    private val workerPool = new BusyWorkerPool(configuration.nWorkerThreadFunction(0))
    private val connections = mutable.HashMap.empty[String, ConnectionContext]

    override val pluginManager: PluginManager = new LinkitPluginManager(configuration.fsAdapter)

    override def getConnection(identifier: String): ConnectionContext = {
        val opt = connections.get(identifier)
        if (opt.isEmpty)
            throw new IllegalArgumentException(s"No connection found for identifier $identifier")
        opt.get
    }

    override def runLater(task: => Unit): Unit = workerPool.runLater(task)

    def newConnection(config: ClientConnectionConfiguration): ConnectionContext = {
        val address = config.remoteAddress
        val dynamicSocket = new ClientDynamicSocket(address, config.socketFactory)
        dynamicSocket.reconnectionPeriod = config.reconnectionPeriod
        openConnection(dynamicSocket, config)
    }

    def unregister(connectionContext: ConnectionContext): Unit = {
        import connectionContext.identifier

        connections.remove(identifier)

        val newThreadCount = configuration.nWorkerThreadFunction(connections.size)
        workerPool.setThreadCount(newThreadCount)

        ContextLogger.info(s"connection '$identifier' bound to $boundIdentifier was detached from application.")
    }

    private def openConnection(socket: DynamicSocket, config: ClientConnectionConfiguration): ConnectionContext = {

    }
}
