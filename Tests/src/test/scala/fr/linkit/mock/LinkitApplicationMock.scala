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

package fr.linkit.mock

import fr.linkit.api.application.config.ApplicationConfiguration
import fr.linkit.api.application.connection.{ConnectionContext, ExternalConnection}
import fr.linkit.api.application.resource.local.ResourceFolder
import fr.linkit.api.internal.system.{Version, Versions}
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.{ClientApplicationConfigBuilder, ClientConnectionConfiguration}
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.application.resource.local.LocalResourceFolder
import fr.linkit.engine.internal.concurrency.pool.{AbstractWorkerPool, SimpleClosedWorkerPool}
import fr.linkit.server.ServerApplication
import fr.linkit.server.config.{ServerApplicationConfigBuilder, ServerConnectionConfiguration}
import fr.linkit.server.connection.ServerConnection

import scala.collection.mutable
import scala.util.{Failure, Success}

class LinkitApplicationMock private(configuration: ApplicationConfiguration,
                                    appResources: ResourceFolder) extends LinkitApplication(configuration, appResources) {

    import LocalResourceFolder.self
    private val clientSideApp = new ClientApplication(new ClientApplicationConfigBuilder {
        override val resourcesFolder: String = appResources.getLocation
    }.buildConfig(), appResources.getOrOpen[ResourceFolder]("ClientTests"))
    private val serverSideApp = new ServerApplication(new ServerApplicationConfigBuilder {
        override val resourcesFolder: String = appResources.getLocation
    }.buildConfig(), appResources.getOrOpen[ResourceFolder]("ServerTests"))

    override protected val appPool : AbstractWorkerPool = new SimpleClosedWorkerPool(5, "Test Pool")
    override           val versions: Versions           = Versions.Unknown

    private val connectionsID   = mutable.HashMap.empty[String, ConnectionContext]
    private val connectionsPort = mutable.HashMap.empty[Int, ConnectionContext]

    start()

    override def countConnections: Int = connectionsID.size

    override def shutdown(): Unit = {
        ()
    }

    override def listConnections: Iterable[ConnectionContext] = connectionsID.values

    override def findConnection(identifier: String): Option[ConnectionContext] = connectionsID.get(identifier)

    override def findConnection(port: Int): Option[ConnectionContext] = connectionsPort.get(port)

    override protected[linkit] def start(): Unit = {
        runLaterControl {
            super.start()
            clientSideApp.runLater {
                clientSideApp.start()
                serverSideApp.runLater(serverSideApp.start())
            }
        }.join()
    }

    def openServerConnection(config: ServerConnectionConfiguration): ServerConnection = {

        serverSideApp.runLaterControl {
            serverSideApp.openServerConnection(config)
        }.join() match {
            case Failure(e)                => throw e
            case Success(serverConnection) =>
                val port       = config.port
                val identifier = config.identifier
                connectionsID(identifier) = serverConnection
                connectionsPort(port) = serverConnection
                serverConnection
        }
    }

    def openClientConnection(config: ClientConnectionConfiguration): ExternalConnection = {
        clientSideApp.runLaterControl {
            clientSideApp.openConnection(config)
        }.join() match {
            case Failure(e)                => throw e
            case Success(clientConnection) =>
                val port       = config.remoteAddress.getPort
                val identifier = config.identifier
                connectionsID(identifier) = clientConnection
                connectionsPort(port) = clientConnection
                clientConnection
        }
    }

}

object LinkitApplicationMock {

    def launch(configuration: ApplicationConfiguration): LinkitApplicationMock = {
        val resource = LinkitApplication.prepareApplication(Version.Unknown, configuration, Seq(getClass))
        new LinkitApplicationMock(configuration, resource)
    }
}
