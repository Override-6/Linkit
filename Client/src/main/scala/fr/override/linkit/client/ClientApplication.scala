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

package fr.`override`.linkit.client

import fr.`override`.linkit.api.connection.{ConnectionContext, ConnectionException, ConnectionInitialisationException, ExternalConnection}
import fr.`override`.linkit.api.local.ApplicationContext
import fr.`override`.linkit.api.local.concurrency.{Procrastinator, workerExecution}
import fr.`override`.linkit.api.local.plugin.PluginManager
import fr.`override`.linkit.api.local.system.AppException
import fr.`override`.linkit.api.local.system.config.ApplicationInstantiationException
import fr.`override`.linkit.client.config.{ClientApplicationConfiguration, ClientConnectionConfiguration}
import fr.`override`.linkit.client.connection.{ClientConnection, ClientDynamicSocket}
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool
import fr.`override`.linkit.core.local.plugin.LinkitPluginManager
import fr.`override`.linkit.core.local.system.ContextLogger

import scala.collection.mutable
import scala.util.control.NonFatal

class ClientApplication private(override val configuration: ClientApplicationConfiguration) extends ApplicationContext with Procrastinator {

    private val workerPool = new BusyWorkerPool(configuration.nWorkerThreadFunction(0), "Application")
    private val connectionCache = mutable.HashMap.empty[Any, ExternalConnection]
    @volatile private var alive: Boolean = false

    override val pluginManager: PluginManager = new LinkitPluginManager(this, configuration.fsAdapter)

    override def runLater(@workerExecution task: => Unit): Unit = workerPool.runLater(task)

    override def countConnections: Int = connectionCache.size

    @workerExecution
    override def shutdown(): Unit = {
        workerPool.checkCurrentThreadOwned("Shutdown must be performed into Application's pool")
        ensureAlive()
        ContextLogger.info("Client application is shutting down...")

        listConnections.foreach(connection => {
            try {
                //Connections will unregister themself from connectionCache automatically
                connection.shutdown()
            } catch {
                case NonFatal(e) => e.printStackTrace()
            }
        })
    }

    override def isAlive: Boolean = alive

    override def listConnections: Iterable[ConnectionContext] = connectionCache.values.toSet

    @workerExecution
    def start(): Unit = {
        workerPool.checkCurrentThreadOwned("Start must be performed into Application's pool")
        if (alive) {
            throw new AppException("Client is already started")
        }
        alive = true
        ContextLogger.info("Starting")
        val pluginFolder = {
            val path = configuration.pluginFolder
            val adapter = configuration.fsAdapter.getAdapter(path)
            adapter.getAbsolutePath
        }
        if (pluginFolder != null && !pluginFolder.isEmpty) {
            val pluginCount = pluginManager.loadAll(pluginFolder).length
            configuration.fsAdapter.getAdapter(pluginFolder)

            ContextLogger.trace(s"Loaded $pluginCount plugins from main plugin folder $pluginFolder")
        }
    }

    def getConnection(identifier: String): ExternalConnection = {
        val opt = connectionCache.get(identifier)
        if (opt.isEmpty)
            throw new IllegalArgumentException(s"No connection found for identifier $identifier")
        opt.get
    }

    @throws[ConnectionInitialisationException]("If something went wrong during the connection's opening")
    @workerExecution
    def newConnection(config: ClientConnectionConfiguration): ExternalConnection = {
        workerPool.checkCurrentThreadOwned("Connection creation must be executed by the client application's thread pool")

        val identifier = config.identifier
  //      if (!Rules.IdentifierPattern.matcher(identifier).matches())
//            throw new ConnectionInitialisationException("Provided identifier does not matches Client's rules.")

        ContextLogger.info(s"Creating connection with address '${config.remoteAddress}'")
        val address = config.remoteAddress
        val dynamicSocket = new ClientDynamicSocket(address, config.socketFactory)
        dynamicSocket.reconnectionPeriod = config.reconnectionMillis
        dynamicSocket.connect("UnknownServerIdentifier")
        workerPool.setThreadCount(configuration.nWorkerThreadFunction(countConnections + 1)) //expand the pool for the new connection that will be opened

        val connection = try {
            ClientConnection.open(dynamicSocket, this, config)
        } catch {
            case e: ConnectionException => throw e
            case NonFatal(e) => throw new ConnectionInitialisationException(s"Could not open connection with server $address : ${e.getMessage}", e)
        }

        val serverIdentifier: String = connection.boundIdentifier
        val port = config.remoteAddress.getPort

        connectionCache.put(serverIdentifier, connection)
        connectionCache.put(port, connection)
        ContextLogger.info(s"Connection Sucessfully bound to $address ($serverIdentifier)")
        connection
    }

    @throws[NoSuchElementException]("If no connection is found into the application's cache.")
    def unregister(connectionContext: ExternalConnection): Unit = {
        import connectionContext.{boundIdentifier, supportIdentifier}

        connectionCache.remove(supportIdentifier)

        val newThreadCount = configuration.nWorkerThreadFunction(connectionCache.size)
        workerPool.setThreadCount(newThreadCount)

        ContextLogger.info(s"Connection '$supportIdentifier' bound to $boundIdentifier was detached from application.")
    }

    private def ensureAlive(): Unit = {
        if (!alive)
            throw new IllegalStateException("Client Application is shutdown.")
    }
}

object ClientApplication {
    @volatile private var initialized = false

    def launch(config: ClientApplicationConfiguration): ClientApplication = {
        if (initialized)
            throw new IllegalStateException("Client Application is already launched.")

        val clientApp = try {
            ContextLogger.info("Instantiating Client application...")
            new ClientApplication(config)
        } catch {
            case NonFatal(e) =>
                throw new ApplicationInstantiationException("Could not instantiate Client Application.", e)
        }

        @volatile var exception: Throwable = null
        clientApp.runLater {
            try {
                ContextLogger.info("Starting Client Application...")
                clientApp.start()
                val loadSchematic = config.loadSchematic
                ContextLogger.trace(s"Applying schematic '${loadSchematic.name}'...")
                loadSchematic.setup(clientApp)
                ContextLogger.trace("Schematic applied successfully.")
            } catch {
                case NonFatal(e) =>
                    exception = e
                case e =>
                    exception = e
                    throw e
            } finally {
                clientApp.synchronized {
                    clientApp.notify()
                }
            }
        }

        clientApp.synchronized {
            clientApp.wait()
        }

        if (exception != null)
            throw new ApplicationInstantiationException("Could not instantiate Client Application.", exception)

        initialized = true
        clientApp
    }
}
