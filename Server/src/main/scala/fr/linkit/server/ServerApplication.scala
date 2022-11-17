/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.server

import fr.linkit.api.application.config.ApplicationInstantiationException
import fr.linkit.api.application.connection.{ConnectionInitialisationException, NoSuchConnectionException}
import fr.linkit.api.application.resource.local.ResourceFolder
import fr.linkit.api.internal.system
import fr.linkit.api.internal.system._
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.api.internal.system.security.ConnectionSecurityException
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.internal.concurrency.VirtualProcrastinator
import fr.linkit.engine.internal.system.{EngineConstants, Rules, StaticVersions}
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerApplicationConfiguration, ServerConnectionConfiguration}
import fr.linkit.server.connection.ServerConnection

import scala.collection.mutable
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class ServerApplication(configuration: ServerApplicationConfiguration, resources: ResourceFolder) extends LinkitApplication(configuration, resources) with ServerApplicationContext {

    protected override val appPool            = VirtualProcrastinator("Application")
    private            val serverCache        = mutable.HashMap.empty[Any, ServerConnection]
    private            val securityManager    = configuration.securityManager
    override           val versions: Versions = StaticVersions(ApiConstants.Version, EngineConstants.Version, ServerApplication.Version)

    override def countConnections: Int = {
        /*
         * We need to divide the servers map size by two because servers connections are twice put into this map,
         * once for port association, and once for identifier association.
         */
        serverCache.size / 2
    }

    override def shutdown(): Unit = this.synchronized {
        /*
        * This method is synchronized on the current application's instance
        * in order to ensure that multiple shutdown aren't executed
        * on the same application, wich could occur to some problems.
        * */
        ensureAlive()
        AppLoggers.App.info("Server application is shutting down...")

        val totalConnectionCount = countConnections
        var downCount            = 0

        listConnections.foreach((serverConnection: ServerConnection) => serverConnection.runLater {
            wrapCloseAction(s"Server connection ${serverConnection.currentName}") {
                serverConnection.shutdown()
            }
            downCount += 1
        })

        alive = false
        AppLoggers.App.info("Server application successfully shutdown.")
    }

    override def listConnections: Iterable[ServerConnection] = {
        serverCache.values.toSet
    }

    override def openServerConnection(configuration: ServerConnectionConfiguration): ServerConnection = /*this.synchronized*/ {

        ensureAlive()
        if (configuration.identifier.length > Rules.MaxConnectionIDLength)
            throw new IllegalArgumentException(s"Server identifier length > ${Rules.MaxConnectionIDLength}")

        securityManager.checkConnectionConfig(configuration)
        AppLoggers.App.debug("Instantiating server connection...")
        val serverConnection = new ServerConnection(this, configuration)

        Try(serverConnection.runLater {
            AppLoggers.App.debug("Starting server...")
            serverConnection.start()
            AppLoggers.App.debug("Server started !")
        }.get) match {
            case Failure(e) => throw new ConnectionInitialisationException(s"Failed to create server connection ${configuration.identifier} on port ${configuration.port}", e)
            case Success(_) =>
        }

        try {
            securityManager.checkConnection(serverConnection)
        } catch {
            case e: ConnectionSecurityException =>
                serverConnection.runLater {
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
    override def unregister(serverConnection: ServerConnection): Unit = {
        ensureAlive()

        val configuration = serverConnection.configuration
        val port          = configuration.port
        val identifier    = configuration.identifier

        if (!serverCache.contains(port) || !serverCache.contains(identifier))
            throw NoSuchConnectionException(s"Could not unregister server $identifier opened on port $port; connection not found in application context.")

        serverCache.remove(port)
        serverCache.remove(identifier)
    }

    override def findConnection(identifier: String): Option[ServerConnection] = {
        serverCache.get(identifier)
    }

    override def findConnection(port: Int): Option[ServerConnection] = {
        serverCache.get(port)
    }

}

object ServerApplication {

    @volatile private var initialized = false
    val Version: Version = system.Version(name = "Server", "1.0.0", false)

    def launch(config: ServerApplicationConfiguration, otherSources: Class[_]*): ServerApplicationContext = this.synchronized {
        if (initialized)
            throw new IllegalStateException("ServerApplication was already launched !")

        val appResources = LinkitApplication.prepareApplication(Version, config, otherSources)

        val serverAppContext = try {
            new ServerApplication(config, appResources)
        } catch {
            case NonFatal(e) =>
                throw new ApplicationInstantiationException("Could not instantiate Server Application.", e)
        }

        Try(serverAppContext.runLater {
            AppLoggers.App.info("Starting Server Application...")
            serverAppContext.start()
            val loadSchematic = config.loadSchematic
            AppLoggers.App.debug(s"Applying schematic '${loadSchematic.name}'...")
            loadSchematic.setup(serverAppContext)
            AppLoggers.App.debug("Schematic applied successfully.")
        }.get()) match {
            case Failure(exception) => throw new ApplicationInstantiationException("Could not instantiate Server Application.", exception)
            case Success(_)         =>
                initialized = true
                serverAppContext
        }
    }

    def launch(builder: ServerApplicationConfigBuilder, caller: Class[_]): ServerApplicationContext = {
        launch(builder.buildConfig(), caller)
    }
}
