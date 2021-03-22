/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.server

import fr.`override`.linkit.api.connection.NoSuchConnectionException
import fr.`override`.linkit.api.local.ApplicationContext
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.plugin.PluginManager
import fr.`override`.linkit.api.local.system.security.ConnectionSecurityException
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool
import fr.`override`.linkit.core.local.plugin.LinkitPluginManager
import fr.`override`.linkit.core.local.system.ContextLogger
import fr.`override`.linkit.server.config.{ServerApplicationConfiguration, ServerConnectionConfiguration}
import fr.`override`.linkit.server.connection.ServerConnection

import scala.collection.mutable

class ServerApplicationContext(override val configuration: ServerApplicationConfiguration) extends ApplicationContext {

    override val pluginManager: PluginManager = null // new LinkitPluginManager(configuration.fsAdapter)
    private val mainWorkerPool = new BusyWorkerPool(3)
    private val serverCache = mutable.HashMap.empty[Any, ServerConnection]
    private val securityManager = configuration.securityManager

    override def countConnections: Int = {
        /*
         * We need to divide the servers map size by two because servers are twice put into this map,
         * once for port association, and once for identifier association.
         */
        serverCache.size / 2
    }

    @workerExecution
    override def shutdown(): Unit = {
        ContextLogger.info("Server application is shutting down...")
        serverCache.values.
            filter(_.isInstanceOf[Int])
            .foreach(_.shutdown())
        ContextLogger.info("Server application successfully shut down.")
    }

    override def runLater(@workerExecution task: => Unit): Unit = mainWorkerPool.runLater(task)

    @workerExecution
    def openServerConnection(configuration: ServerConnectionConfiguration): ServerConnection = {
        BusyWorkerPool.checkCurrentIsWorker("openning server connection must be done in a worker thread pool.")
        securityManager.checkConnectionConfig(configuration)
        val serverConnection = new ServerConnection(this, configuration)
        serverConnection.start()

        try {
            securityManager.checkConnection(serverConnection)
        } catch {
            case e: ConnectionSecurityException =>
                serverConnection.shutdown()
                throw e
        }

        val port = configuration.port
        val identifier = configuration.identifier
        serverCache.put(port, serverConnection)
        serverCache.put(identifier, serverConnection)

        serverConnection
    }

    @throws[NoSuchConnectionException]("If the connection isn't found in the application's cache.")
    def unregister(serverConnection: ServerConnection): Unit = {
        val configuration = serverConnection.configuration
        val port = configuration.port
        val identifier = configuration.identifier

        if (!serverCache.contains(port) || !serverCache.contains(identifier))
            throw NoSuchConnectionException(s"Could not unregister server $identifier opened on port $port; connection not found in application context.")

        serverCache.remove(port)
        serverCache.remove(identifier)
    }

    def getServerConnection(identifier: String): Option[ServerConnection] = {
        serverCache.get(identifier)
    }

    def getServerConnection(port: Int): Option[ServerConnection] = {
        serverCache.get(port)
    }
}
