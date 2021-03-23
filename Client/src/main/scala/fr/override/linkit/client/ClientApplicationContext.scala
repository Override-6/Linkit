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

import fr.`override`.linkit.api.connection.{ConnectionInitialisationException, ExternalConnection}
import fr.`override`.linkit.api.local.ApplicationContext
import fr.`override`.linkit.api.local.concurrency.{Procrastinator, workerExecution}
import fr.`override`.linkit.api.local.plugin.PluginManager
import fr.`override`.linkit.client.config.{ClientApplicationConfiguration, ClientConnectionConfiguration}
import fr.`override`.linkit.core.connection.packet.traffic.DynamicSocket
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool
import fr.`override`.linkit.core.local.plugin.LinkitPluginManager
import fr.`override`.linkit.core.local.system.ContextLogger

import scala.collection.mutable

class ClientApplicationContext(override val configuration: ClientApplicationConfiguration) extends ApplicationContext with Procrastinator {

    private val workerPool = new BusyWorkerPool(configuration.nWorkerThreadFunction(0), "Client Application")
    private val connections = mutable.HashMap.empty[String, ExternalConnection]

    override val pluginManager: PluginManager = new LinkitPluginManager(configuration.fsAdapter)

    override def runLater(@workerExecution task: => Unit): Unit = workerPool.runLater(task)

    override def countConnections: Int = connections.size

    def getConnection(identifier: String): ExternalConnection = {
        val opt = connections.get(identifier)
        if (opt.isEmpty)
            throw new IllegalArgumentException(s"No connection found for identifier $identifier")
        opt.get
    }

    @throws[ConnectionInitialisationException]("If something went wrong during the connection's opening")
    @workerExecution
    def newConnection(config: ClientConnectionConfiguration): ExternalConnection = {
        val address = config.remoteAddress
        val dynamicSocket = new ClientDynamicSocket(address, config.socketFactory)
        dynamicSocket.reconnectionPeriod = config.reconnectionPeriod
        workerPool.setThreadCount(countConnections + 1) //expand the pool for the new connection that will be opened

        openConnection(dynamicSocket, config)
    }

    @throws[NoSuchElementException]("If the connection isn't found in the application's cache.")
    def unregister(connectionContext: ExternalConnection): Unit = {
        import connectionContext.{boundIdentifier, supportIdentifier}

        connections.remove(supportIdentifier)

        val newThreadCount = configuration.nWorkerThreadFunction(connections.size)
        workerPool.setThreadCount(newThreadCount)

        ContextLogger.info(s"connection '$supportIdentifier' bound to $boundIdentifier was detached from application.")
    }

    private def openConnection(socket: DynamicSocket, config: ClientConnectionConfiguration): ExternalConnection = {

    }

}
