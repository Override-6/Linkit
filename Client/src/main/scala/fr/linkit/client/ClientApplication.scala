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

package fr.linkit.client

import fr.linkit.api.application.config.ApplicationInstantiationException
import fr.linkit.api.application.connection.{ConnectionInitialisationException, ExternalConnection}
import fr.linkit.api.application.resource.local.ResourceFolder
import fr.linkit.api.internal.concurrency.Procrastinator
import fr.linkit.api.internal.system
import fr.linkit.api.internal.system._
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.client.ClientApplication.Version
import fr.linkit.client.config.{ClientApplicationConfiguration, ClientConnectionConfiguration}
import fr.linkit.client.connection.{ClientConnection, ClientDynamicSocket}
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.internal.system.{EngineConstants, Rules, StaticVersions}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class ClientApplication(configuration: ClientApplicationConfiguration, resources: ResourceFolder) extends LinkitApplication(configuration, resources) with ClientApplicationContext {
    
    override protected val appPool             = Procrastinator("Application")
    private            val connectionCache     = mutable.HashMap.empty[Any, ExternalConnection]
    @volatile private var connectionCount: Int = 0
    
    override def countConnections: Int = connectionCount
    
    override val versions: Versions = StaticVersions(ApiConstants.Version, EngineConstants.Version, Version)
    
    override def shutdown(): Unit = {
        ensureAlive()
        AppLoggers.App.info("Client application is shutting down...")
        
        listConnections.foreach(connection => {
            try {
                //Connections will unregister themself from connectionCache automatically
                connection.shutdown()
                connectionCount -= 1
            } catch {
                case NonFatal(e) => e.printStackTrace()
            }
        })
    }
    
    override def listConnections: Iterable[ExternalConnection] = connectionCache.values.toSet
    
    override def findConnection(identifier: String): Option[ExternalConnection] = {
        connectionCache.get(identifier).orElse(connectionCache.find(_._2.boundIdentifier == identifier).map(_._2))
    }
    
    override def findConnection(port: Int): Option[ExternalConnection] = {
        connectionCache.values.find(_.port == port)
    }
    
    @throws[ConnectionInitialisationException]("If something went wrong during the connection's opening")
    override def openConnection(config: ClientConnectionConfiguration): ExternalConnection = {

        val identifier = config.identifier
        if (!Rules.IdentifierPattern.matcher(identifier).matches())
            throw new ConnectionInitialisationException("Provided identifier does not matches Client's rules.")
        
        connectionCount += 1

        AppLoggers.App.info(s"Creating connection to address '${config.remoteAddress}'...")
        val address       = config.remoteAddress
        val dynamicSocket = new ClientDynamicSocket(address, config.socketFactory)
        dynamicSocket.reconnectionPeriod = config.reconnectionMillis
        dynamicSocket.connect("UnknownServerIdentifier")
        AppLoggers.App.trace("Socket accepted !")
        
        val connection = try {
            ClientConnection.open(dynamicSocket, this, config)
        } catch {
            case NonFatal(e) =>
                connectionCount -= 1
                throw new ConnectionInitialisationException(s"Could not open connection with server $address : ${e.getMessage}", e)
        }
        
        connectionCache.put(identifier, connection)
        
        val serverIdentifier: String = connection.boundIdentifier
        AppLoggers.App.info(s"Connection Sucessfully bound to $address ($serverIdentifier)")
        connection
    }
    
    @throws[NoSuchElementException]("If no connection is found into the application's cache.")
    override def unregister(connectionContext: ExternalConnection): Unit = {
        import connectionContext.{boundIdentifier, currentIdentifier}
        
        connectionCache.remove(currentIdentifier)
        connectionCount -= 1
        //val newThreadCount = Math.max(configuration.nWorkerThreadFunction(connectionCount), 1)
        //appPool.setThreadCount(newThreadCount)
        
        AppLoggers.App.info(s"Connection '$currentIdentifier' bound to $boundIdentifier was detached from application.")
    }
    
}

object ClientApplication {
    
    val Version: Version = system.Version("Client", "1.0.0", false)
    
    @volatile private var initialized = false
    
    def launch(config: ClientApplicationConfiguration, otherSources: Class[_]*): ClientApplicationContext = {
        if (initialized)
            throw new IllegalStateException("Client Application is already launched.")
        
        val resources = LinkitApplication.prepareApplication(Version, config, otherSources)
        
        val clientApp = try {
            AppLoggers.App.info("Instantiating Client application...")
            new ClientApplication(config, resources)
        } catch {
            case NonFatal(e) =>
                throw new ApplicationInstantiationException("Could not instantiate Client Application.", e)
        }
        
        Await.ready(clientApp.runLater {
            AppLoggers.App.info("Starting Client Application...")
            clientApp.start()
            val loadSchematic = config.loadSchematic
            AppLoggers.App.debug(s"Applying schematic '${loadSchematic.name}'...")
            loadSchematic.setup(clientApp)
            AppLoggers.App.trace("Schematic applied successfully.")
        }, Duration.Inf).value.get match {
            case Failure(e) =>
                throw new ApplicationInstantiationException("Could not instantiate Client Application.", e)
            case Success(_) =>
                initialized = true
                clientApp
        }
    }
}