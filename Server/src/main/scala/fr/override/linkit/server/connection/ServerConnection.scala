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

package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.network.Network
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.connection.packet.traffic.ChannelScope.ScopeFactory
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectable, PacketInjectableFactory, PacketTraffic}
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.system.event.EventNotifier
import fr.`override`.linkit.core.connection.packet.fundamental.RefPacket.StringPacket
import fr.`override`.linkit.core.connection.packet.fundamental.ValPacket.BooleanPacket
import fr.`override`.linkit.core.connection.packet.traffic.DynamicSocket
import fr.`override`.linkit.core.local.concurrency.BusyWorkerPool
import fr.`override`.linkit.core.local.system.ContextLogger
import fr.`override`.linkit.core.local.system.event.DefaultEventNotifier
import fr.`override`.linkit.server.config.{AmbiguityStrategy, ServerConnectionConfiguration}
import fr.`override`.linkit.server.network.ServerNetwork
import fr.`override`.linkit.server.{ServerApplicationContext, ServerException, ServerPacketTraffic}
import java.net.{ServerSocket, SocketException}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

class ServerConnection(applicationContext: ServerApplicationContext,
                       override val configuration: ServerConnectionConfiguration) extends ConnectionContext {

    private val workerPool: BusyWorkerPool = new BusyWorkerPool(configuration.nWorkerThreadFunction(0))
    private val serverSocket: ServerSocket = new ServerSocket(configuration.port)
    private val connectionsManager: ExternalConnectionsManager = new ExternalConnectionsManager(this)

    override val supportIdentifier: String = configuration.identifier
    override val traffic: PacketTraffic = new ServerPacketTraffic(this)
    private[server] val serverNetwork: ServerNetwork = new ServerNetwork(this, traffic)
    override val network: Network = serverNetwork
    override val eventNotifier: EventNotifier = new DefaultEventNotifier
    override val translator: PacketTranslator = configuration.translator

    @volatile private var alive = false

    @workerExecution
    override def shutdown(): Unit = {
        BusyWorkerPool.checkCurrentIsWorker("Must shutdown server connection in a worker thread.")
        if (!alive)
            return

        val port = configuration.port

        ContextLogger.info(s"Server '$supportIdentifier' on port $port prepares to shutdown...")
        applicationContext.unregister(this)

        connectionsManager.close()
        alive = false
        ContextLogger.info(s"Server '$supportIdentifier' shutdown.")
    }

    @workerExecution
    override def start(): Unit = {
        BusyWorkerPool.checkCurrentIsWorker("Must start server connection in a worker thread.")
        if (alive)
            throw new ServerException(this, "Server is already started.")
        ContextLogger.info(s"Starting server '$supportIdentifier' on port ${configuration.port}...")
        ContextLogger.info(s"Identifier Ambiguity Strategy : ${configuration.identifierAmbiguityStrategy}")

        try {
            loadSocketListener()
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                shutdown()
        }
    }

    override def isAlive: Boolean = alive

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        traffic.getInjectable(injectableID, scopeFactory, factory)
    }

    override def runLater(@workerExecution task: => Unit): Unit = workerPool.runLater(task)

    def getConnection(identifier: String): Option[ServerExternalConnection] = Option(connectionsManager.getConnection(identifier))

    def broadcastPacketToConnections(packet: Packet, sender: String, injectableID: Int, discarded: String*): Unit = {
        if (connectionsManager.countConnected - discarded.length <= 0) {
            // There is nowhere to send this packet.
            return
        }
        connectionsManager.broadcastBytes(packet, injectableID, sender, discarded.appended(supportIdentifier): _*)
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////  C L I E N T  I N I T I A L I S A T I O N  H A N D L I N G  /////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //TODO Documentation

    /**
     * Reads a welcome packet from a connection.<br>
     * The Welcome packet is the first packet that a connection must send in order to provide his identifier to the server.
     *
     * @return the identifier bound with the socket
     * */
    private def readWelcomePacket(socket: SocketContainer): String = {
        val welcomePacketLength = socket.readInt()
        if (welcomePacketLength > 32)
            throw new ConnectionInitialisationException("Relay identifier exceeded maximum size limit of 32")

        val welcomePacket = socket.read(welcomePacketLength)
        new String(welcomePacket)
    }

    private def listenSocketConnection(): Unit = {
        val socketContainer = new SocketContainer(true)
        try {
            val clientSocket = serverSocket.accept()
            socketContainer.set(clientSocket)
            runLater {
                handleSocket(socketContainer)
            }
        } catch {
            case e: SocketException =>
                val msg = e.getMessage.toLowerCase
                if (msg == "socket closed" || msg == "socket is closed")
                    return
                Console.err.println(msg)
                onException(e)
            case NonFatal(e) =>
                e.printStackTrace()
                onException(e)
        }

        def onException(e: Throwable): Unit = {
            sendRefusedConnection(socketContainer, s"An exception occurred in server during client connection initialisation ($e)") //sends a negative response for the fr.override.linkit.client initialisation handling
            shutdown()
        }
    }

    private def handleSocket(socket: SocketContainer): Unit = {
        val identifier = readWelcomePacket(socket)
        socket.identifier = identifier
        handleRelayConnection(identifier, socket)
    }

    private def loadSocketListener(): Unit = {
        val thread = new Thread(() => {
            alive = true
            while (alive) listenSocketConnection()
        })
        thread.setName("Socket Connection Listener")
        thread.start()
    }

    private def handleRelayConnection(identifier: String,
                                      socket: SocketContainer): Unit = {

        val currentConnection = getConnection(identifier)
        //There is no currently connected connection with the same identifier on this network.
        if (currentConnection.isEmpty) {
            connectionsManager.createConnection(identifier, socket, null) //TODO
            val newConnection = getConnection(identifier)

            if (newConnection.isDefined) //may be empty, in this case, the connection would be rejected.
                serverNetwork.addEntity(newConnection.get)
            return
        }

        handleConnectionIdAmbiguity(currentConnection.get, socket)
    }

    private def handleConnectionIdAmbiguity(current: ServerExternalConnection,
                                            socket: SocketContainer): Unit = {

        if (!current.isConnected) {
            current.updateSocket(socket.get)
            sendAuthorisedConnection(socket)
            println(s"The connection of ${current.boundIdentifier} has been resumed.")
            return
        }

        val identifier = current.boundIdentifier
        val rejectMsg = s"Another relay point with id '$identifier' is currently connected on the targeted network."
        import AmbiguityStrategy._
        configuration.identifierAmbiguityStrategy match {
            case CLOSE_SERVER =>
                sendRefusedConnection(socket, rejectMsg + " Consequences: Closing Server...")
                //broadcastMessage(true, "RelayServer will close your connection because of a critical error")
                shutdown()

            case REJECT_NEW =>
                Console.err.println("Rejected connection of a client because it gave an already registered relay identifier.")
                sendRefusedConnection(socket, rejectMsg)

            case REPLACE =>
                connectionsManager.unregister(identifier).get.shutdown()
                connectionsManager.createConnection(identifier, socket, null)
            //The connection initialisation packet isn't sent here because it is send into the registerConnection method.

            case DISCONNECT_BOTH =>
                connectionsManager.unregister(identifier).get.shutdown()
                sendRefusedConnection(socket, rejectMsg + " Consequences : Disconnected both")
        }
    }

    private[connection] def sendAuthorisedConnection(socket: DynamicSocket): Unit = {
        val coordinates = DedicatedPacketCoordinates(PacketTraffic.SystemChannelID, "unknown", supportIdentifier)
        socket.write(translator.translate(BooleanPacket(true), coordinates))
    }

    private[connection] def sendRefusedConnection(socket: DynamicSocket, message: String): Unit = {
        val coordinates = DedicatedPacketCoordinates(PacketTraffic.SystemChannelID, "unknown", supportIdentifier)
        socket.write(translator.translate(BooleanPacket(false), coordinates))
        socket.write(translator.translate(StringPacket(message), coordinates))
    }

}
