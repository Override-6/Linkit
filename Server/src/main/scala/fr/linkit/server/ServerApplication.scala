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

package fr.linkit.server

import fr.linkit.api.connection.NoSuchConnectionException
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.plugin.PluginManager
import fr.linkit.api.local.system._
import fr.linkit.api.local.system.config.ApplicationInstantiationException
import fr.linkit.api.local.system.security.ConnectionSecurityException
import fr.linkit.api.local.{ApplicationContext, system}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.mapping.ClassMapEngine
import fr.linkit.core.local.plugin.LinkitPluginManager
import fr.linkit.core.local.system.{AbstractCoreConstants, Rules, StaticVersions}
import fr.linkit.server.ServerApplication.Version
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerApplicationConfiguration, ServerConnectionConfiguration}
import fr.linkit.server.connection.ServerConnection

import java.util.concurrent.locks.LockSupport
import scala.collection.mutable
import scala.util.control.NonFatal

class ServerApplication private(override val configuration: ServerApplicationConfiguration) extends ApplicationContext {

    private  val mainWorkerPool               = new BusyWorkerPool(configuration.mainPoolThreadCount, "Application")
    override val pluginManager: PluginManager = new LinkitPluginManager(this, configuration.fsAdapter)
    private  val serverCache                  = mutable.HashMap.empty[Any, ServerConnection]
    private  val securityManager              = configuration.securityManager
    @volatile private var alive               = false

    override val versions: Versions = StaticVersions(ApiConstants.Version, AbstractCoreConstants.Version, Version)

    override def countConnections: Int = {
        /*
         * We need to divide the servers map size by two because servers are twice put into this map,
         * once for port association, and once for identifier association.
         */
        serverCache.size / 2
    }

    @workerExecution
    override def shutdown(): Unit = this.synchronized {
        /*
        * This method is synchronized on the current application's instance
        * in order to ensure that multiple shutdown aren't executed
        * on the same application, wich could occur to some problems.
        * */
        mainWorkerPool.ensureCurrentThreadOwned("Shutdown must be performed into Application's pool")
        ensureAlive()
        AppLogger.info("Server application is shutting down...")
        val totalConnectionCount = countConnections
        var downCount            = 0
        val shutdownThread       = BusyWorkerPool.currentWorker
        val shutdownTask         = shutdownThread.currentTaskID

        listConnections.foreach((serverConnection: ServerConnection) => serverConnection.runLater {
            try {
                serverConnection.shutdown()
            } catch {
                case NonFatal(e) => AppLogger.printStackTrace(e)
            }
            downCount += 1
            if (downCount == totalConnectionCount)
                BusyWorkerPool.unpauseTask(shutdownThread, shutdownTask)
        })
        mainWorkerPool.pauseCurrentTask()

        alive = false
        AppLogger.info("Server application successfully shutdown.")
    }

    @workerExecution
    private def start(): Unit = {
        mainWorkerPool.ensureCurrentThreadOwned("Start must be performed into Application's pool")
        if (alive) {
            throw new AppException("Server is already started")
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

    override def isAlive: Boolean = alive

    override def listConnections: Iterable[ServerConnection] = {
        serverCache.values.toSet
    }

    override def runLater(@workerExecution task: => Unit): Unit = mainWorkerPool.runLater(task)

    override def ensureCurrentThreadOwned(msg: String): Unit = mainWorkerPool.ensureCurrentThreadOwned(msg)

    override def ensureCurrentThreadOwned(): Unit = mainWorkerPool.ensureCurrentThreadOwned()

    override def isCurrentThreadOwned: Boolean = mainWorkerPool.isCurrentThreadOwned

    @workerExecution
    def openServerConnection(configuration: ServerConnectionConfiguration): ServerConnection = /*this.synchronized*/ {
        /*
        * This method is synchronized in order to prone parallel server initializations
        * and to ensure that no server would be open during shutdown.
        * */
        mainWorkerPool.ensureCurrentThreadOwned("Open server connection must be performed into Application's pool.")

        ensureAlive()
        if (configuration.identifier.length > Rules.MaxConnectionIDLength)
            throw new IllegalArgumentException(s"Server identifier length > ${Rules.MaxConnectionIDLength}")

        securityManager.checkConnectionConfig(configuration)
        AppLogger.debug("Instantiating server connection...")
        val serverConnection = new ServerConnection(this, configuration)
        val openerThread     = BusyWorkerPool.currentWorker
        val openerTask       = openerThread.currentTaskID

        serverConnection.runLater {
            AppLogger.debug("Starting server...")
            serverConnection.start()
            AppLogger.debug("Server started !")
            BusyWorkerPool.unpauseTask(openerThread, openerTask)
        }
        mainWorkerPool.pauseCurrentTask()

        try {
            securityManager.checkConnection(serverConnection)
        } catch {
            case e: ConnectionSecurityException =>
                runLater {
                    serverConnection.shutdown()
                }
                throw e
        }

        val port       = configuration.port
        val identifier = configuration.identifier
        serverCache.put(port, serverConnection)
        serverCache.put(identifier, serverConnection)

        serverConnection
    }

    @throws[NoSuchConnectionException]("If the connection isn't found in the application's cache.")
    def unregister(serverConnection: ServerConnection): Unit = {
        ensureAlive()

        val configuration = serverConnection.configuration
        val port          = configuration.port
        val identifier    = configuration.identifier

        if (!serverCache.contains(port) || !serverCache.contains(identifier))
            throw NoSuchConnectionException(s"Could not unregister server $identifier opened on port $port; connection not found in application context.")

        serverCache.remove(port)
        serverCache.remove(identifier)
    }

    override def getConnection(identifier: String): Option[ServerConnection] = {
        serverCache.get(identifier)
    }

    override def getConnection(port: Int): Option[ServerConnection] = {
        serverCache.get(port)
    }

    private def ensureAlive(): Unit = {
        if (!alive)
            throw new IllegalStateException("Server Application is shutdown.")
    }

}

object ServerApplication {

    @volatile private var initialized = false
    val Version: Version = system.Version(name = "Server", "1.0.0", false)

    def launch(config: ServerApplicationConfiguration, otherSources: Class[_]*): ServerApplication = {
        if (initialized)
            throw new IllegalStateException("Application was already launched !")

        AppLogger.info("-------------------------- Linkit Framework --------------------------")
        AppLogger.info(s"\tApi Version            : ${ApiConstants.Version}")
        AppLogger.info(s"\tAbstractCore Version   : ${AbstractCoreConstants.Version}")
        AppLogger.info(s"\tImplementation Version : ${Version}")
        AppLogger.info(s"\tCurrent JDK Version    : ${System.getProperty("java.version")}")

        AppLogger.info("Mapping classes, this task may take a time.")

        val fsa = config.fsAdapter
        ClassMapEngine.mapAllSourcesOfClasses(fsa, otherSources: _*)
        ClassMapEngine.mapAllSourcesOfClasses(fsa, getClass, ClassMapEngine.getClass, Predef.getClass, classOf[ApplicationContext])
        ClassMapEngine.mapJDK(fsa)

        val serverAppContext = try {
            AppLogger.info("Instantiating Server application...")
            new ServerApplication(config)
        } catch {
            case NonFatal(e) =>
                throw new ApplicationInstantiationException("Could not instantiate Server Application.", e)
        }

        val loaderThread                   = Thread.currentThread()
        @volatile var exception: Throwable = null
        serverAppContext.runLater {
            try {
                AppLogger.info("Starting Server Application...")
                serverAppContext.start()
                val loadSchematic = config.loadSchematic
                AppLogger.trace(s"Applying schematic '${loadSchematic.name}'...")
                loadSchematic.setup(serverAppContext)
                AppLogger.trace("Schematic applied successfully.")
            } catch {
                case NonFatal(e) =>
                    exception = e
                    AppLogger.printStackTrace(e)
            }
            LockSupport.unpark(loaderThread)
        }
        LockSupport.park()

        if (exception != null)
            throw new ApplicationInstantiationException("Could not instantiate Server Application.", exception)

        initialized = true
        serverAppContext
    }

    def launch(builder: ServerApplicationConfigBuilder, caller: Class[_]): ServerApplication = {
        launch(builder.buildConfig(), caller)
    }
}
