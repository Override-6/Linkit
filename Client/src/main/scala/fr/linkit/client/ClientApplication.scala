/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.client

import fr.linkit.api.connection.{ConnectionContext, ConnectionException, ConnectionInitialisationException, ExternalConnection}
import fr.linkit.api.local.concurrency.{Procrastinator, workerExecution}
import fr.linkit.api.local.system
import fr.linkit.api.local.system._
import fr.linkit.api.local.system.config.ApplicationInstantiationException
import fr.linkit.client.ClientApplication.Version
import fr.linkit.client.config.{ClientApplicationConfiguration, ClientConnectionConfiguration}
import fr.linkit.client.connection.{ClientConnection, ClientDynamicSocket}
import fr.linkit.core.local.LinkitApplication
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.system.{AbstractCoreConstants, Rules, StaticVersions}

import java.util.concurrent.locks.LockSupport
import scala.collection.mutable
import scala.util.control.NonFatal

class ClientApplication private(override val configuration: ClientApplicationConfiguration) extends LinkitApplication(configuration) with Procrastinator {

    private val workerPool                     = new BusyWorkerPool(configuration.nWorkerThreadFunction(0), "Application")
    private val connectionCache                = mutable.HashMap.empty[Any, ExternalConnection]
    @volatile private var connectionCount: Int = 0

    override def countConnections: Int = connectionCount

    override val versions: Versions = StaticVersions(ApiConstants.Version, AbstractCoreConstants.Version, Version)

    @workerExecution
    override def shutdown(): Unit = {
        workerPool.ensureCurrentThreadOwned("Shutdown must be performed into Application's pool")
        ensureAlive()
        AppLogger.info("Client application is shutting down...")

        listConnections.foreach(connection => {
            try {
                //Connections will unregister themself from connectionCache automatically
                connection.shutdown()
                connectionCount -= 1
            } catch {
                case NonFatal(e) => AppLogger.printStackTrace(e)
            }
        })
    }

    override def listConnections: Iterable[ConnectionContext] = connectionCache.values.toSet

    override def runLater(@workerExecution task: => Unit): Unit = workerPool.runLater(task)

    override def ensureCurrentThreadOwned(msg: String): Unit = workerPool.ensureCurrentThreadOwned(msg)

    override def ensureCurrentThreadOwned(): Unit = workerPool.ensureCurrentThreadOwned()

    override def isCurrentThreadOwned: Boolean = workerPool.isCurrentThreadOwned



    @workerExecution
    private def start(): Unit = {
        workerPool.ensureCurrentThreadOwned("Start must be performed into Application's pool")
        if (alive) {
            throw new AppException("Client is already started")
        }
        alive = true
        val pluginFolder = configuration.pluginFolder match {
            case Some(path) =>
                val adapter = configuration.fsAdapter.getAdapter(path)
                adapter.getAbsolutePath //converting to absolute path.
            case None       => null
        }
        if (pluginFolder != null) {
            val pluginCount = pluginManager.loadAll(pluginFolder).length
            configuration.fsAdapter.getAdapter(pluginFolder)
            AppLogger.trace(s"Loaded $pluginCount plugins from main plugin folder $pluginFolder")
        }
    }

    override def getConnection(identifier: String): Option[ExternalConnection] = {
        connectionCache.get(identifier)
    }

    override def getConnection(port: Int): Option[ExternalConnection] = {
        connectionCache.values.find(_.port == port)
    }

    @throws[ConnectionInitialisationException]("If something went wrong during the connection's opening")
    @workerExecution
    def openConnection(config: ClientConnectionConfiguration): ExternalConnection = {
        workerPool.ensureCurrentThreadOwned("Connection creation must be executed by the client application's thread pool")

        val identifier = config.identifier
        if (!Rules.IdentifierPattern.matcher(identifier).matches())
            throw new ConnectionInitialisationException("Provided identifier does not matches Client's rules.")

        connectionCount += 1
        workerPool.setThreadCount(configuration.nWorkerThreadFunction(connectionCount)) //expand the pool for the new connection that will be opened

        AppLogger.info(s"Creating connection with address '${config.remoteAddress}'")
        val address       = config.remoteAddress
        val dynamicSocket = new ClientDynamicSocket(address, config.socketFactory)
        dynamicSocket.reconnectionPeriod = config.reconnectionMillis
        dynamicSocket.connect("UnknownServerIdentifier")
        AppLogger.trace("Socket accepted !")

        val connection = try {
            ClientConnection.open(dynamicSocket, this, config)
        } catch {
            case e: ConnectionException =>
                connectionCount -= 1
                throw e
            case NonFatal(e)            =>
                runLater {
                    AppLogger.fatal("EXITING...")
                    System.exit(1)
                }
                connectionCount -= 1
                throw new ConnectionInitialisationException(s"Could not open connection with server $address : ${e.getMessage}", e)
        }

        val serverIdentifier: String = connection.boundIdentifier
        val port                     = config.remoteAddress.getPort

        connectionCache.put(serverIdentifier, connection)
        connectionCache.put(port, connection)
        AppLogger.info(s"Connection Sucessfully bound to $address ($serverIdentifier)")
        connection
    }

    @throws[NoSuchElementException]("If no connection is found into the application's cache.")
    def unregister(connectionContext: ExternalConnection): Unit = {
        import connectionContext.{boundIdentifier, supportIdentifier}

        connectionCache.remove(supportIdentifier)
        connectionCount -= 1
        val newThreadCount = configuration.nWorkerThreadFunction(connectionCount)
        workerPool.setThreadCount(newThreadCount)

        AppLogger.info(s"Connection '$supportIdentifier' bound to $boundIdentifier was detached from application.")
    }

}

object ClientApplication {

    val Version: Version = system.Version("Client", "1.0.0", false)

    @volatile private var initialized = false

    def launch(config: ClientApplicationConfiguration, otherSources: Class[_]*): ClientApplication = {
        if (initialized)
            throw new IllegalStateException("Client Application is already launched.")

        LinkitApplication.prepareApplication(Version, config.fsAdapter, otherSources)

        val clientApp = try {
            AppLogger.info("Instantiating Client application...")
            new ClientApplication(config)
        } catch {
            case NonFatal(e) =>
                throw new ApplicationInstantiationException("Could not instantiate Client Application.", e)
        }

        val loaderThread                   = Thread.currentThread()
        @volatile var exception: Throwable = null
        clientApp.runLater {
            try {
                AppLogger.info("Starting Client Application...")
                clientApp.start()
                val loadSchematic = config.loadSchematic
                AppLogger.trace(s"Applying schematic '${loadSchematic.name}'...")
                loadSchematic.setup(clientApp)
                AppLogger.trace("Schematic applied successfully.")
            } catch {
                case NonFatal(e) =>
                    exception = e
            }
            LockSupport.unpark(loaderThread)
        }
        LockSupport.park()

        clientApp.synchronized {
            clientApp.wait()
        }

        if (exception != null)
            throw new ApplicationInstantiationException("Could not instantiate Client Application.", exception)

        initialized = true
        clientApp
    }
}
