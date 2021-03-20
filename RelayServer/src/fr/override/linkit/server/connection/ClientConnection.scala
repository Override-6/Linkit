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

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.concurrency.{PacketWorkerThread, RelayWorkerThreadPool, relayWorkerExecution}
import fr.`override`.linkit.api.exception.{RelayException, UnexpectedPacketException}
import fr.`override`.linkit.api.network.{ConnectionState, RemoteConsole}
import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.fundamental.ValPacket.BooleanPacket
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.packet.traffic.PacketInjections
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.task.TasksHandler

import java.net.Socket
import scala.util.control.NonFatal

class ClientConnection private(session: ClientConnectionSession) extends JustifiedCloseable {

    val identifier: String = session.identifier

    private val server = session.server
    private val packetTranslator = server.packetTranslator
    private val manager: ConnectionsManager = server.connectionsManager

    private val workerThread = new RelayWorkerThreadPool()

    @volatile private var closed = false

    override def close(reason: CloseReason): Unit = {
        RelayWorkerThreadPool.checkCurrentIsWorker()
        closed = true
        if (reason.isInternal && isConnected) {
            val sysChannel = session.channel
            sysChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }
        ConnectionPacketWorker.close(reason)

        session.close(reason)

        manager.unregister(identifier)
        workerThread.close()
        println(s"Connection closed for $identifier")
    }

    def start(): Unit = {
        if (closed) {
            throw new RelayException("This Connection was already used and is now definitely closed.")
        }
        ConnectionPacketWorker.start()
    }

    def sendPacket(packet: Packet, channelID: Int): Unit = {
        runLater {
            val bytes = packetTranslator.fromPacketAndCoords(packet, DedicatedPacketCoordinates(channelID, identifier, server.identifier))
            session.send(bytes)
        }
    }

    def isConnected: Boolean = getState == ConnectionState.CONNECTED

    def getTasksHandler: TasksHandler = session.tasksHandler

    def getConsoleOut: RemoteConsole = session.outConsole

    def getConsoleErr: RemoteConsole = session.errConsole

    def getState: ConnectionState = session.getSocketState

    def addConnectionStateListener(action: ConnectionState => Unit): Unit = session.addStateListener(action)

    def runLater(callback: => Unit): Unit = {
        workerThread.runLater(callback)
    }

    override def isClosed: Boolean = closed

    private[server] def updateSocket(socket: Socket): Unit = {
        RelayWorkerThreadPool.checkCurrentIsWorker()
        session.updateSocket(socket)
    }

    private[connection] def sendBytes(bytes: Array[Byte]): Unit = {
        session.send(PacketUtils.wrap(bytes))
    }

    object ConnectionPacketWorker extends PacketWorkerThread {

        @relayWorkerExecution
        override protected def refresh(): Unit = {
            try {
                session
                        .packetReader
                        .nextPacket((packet, coordinates, packetNumber) => {
                            runLater(handlePacket(packet, coordinates, packetNumber))
                        })
            } catch {
                case NonFatal(e) =>
                    e.printStackTrace()
                    e.printStackTrace(session.errConsole)
                    runLater {
                        ClientConnection.this.close(CloseReason.INTERNAL_ERROR)
                    }
            }
        }

        @relayWorkerExecution
        private def handlePacket(packet: Packet, coordinates: DedicatedPacketCoordinates, number: Int): Unit = {
            if (closed)
                return

            packet match {
                case systemPacket: SystemPacket => handleSystemOrder(systemPacket)
                case init: TaskInitPacket => session.tasksHandler.handlePacket(init, coordinates)
                case _: Packet =>
                    val injection = PacketInjections.createInjection(packet, coordinates, number)
                    session.serverTraffic.handleInjection(injection)
            }
        }


        private def handleSystemOrder(packet: SystemPacket): Unit = {
            val orderType = packet.order
            val reason = packet.reason.reversedPOV()
            val content = packet.content

            import SystemOrder._
            orderType match {
                case CLIENT_CLOSE => runLater(ClientConnection.this.close(reason))
                case SERVER_CLOSE => server.close(reason)
                case ABORT_TASK => session.tasksHandler.skipCurrent(reason)
                case CHECK_ID => checkIDRegistered(new String(content))
                case PRINT_INFO => server.getConsoleOut(identifier).println(s"Connected to server ${server.relayVersion} (${Relay.ApiVersion})")

                case _ => new UnexpectedPacketException(s"Could not complete order '$orderType', can't be handled by a server or unknown order")
                        .printStackTrace(getConsoleErr)
            }

            def checkIDRegistered(target: String): Unit = {
                session.channel.send(BooleanPacket(server.isConnected(target)))
            }
        }
    }

}

object ClientConnection {

    /**
     * Constructs a ClientConnection without starting it.
     *
     * @throws NullPointerException if the identifier or the socket is null.
     * @return a started ClientConnection.
     * @see [[SocketContainer]]
     * */
    def open(@NotNull session: ClientConnectionSession): ClientConnection = {
        if (session == null) {
            throw new NullPointerException("Unable to construct ClientConnection : session cant be null")
        }
        val connection = new ClientConnection(session)
        connection.start()
        connection
    }

}
