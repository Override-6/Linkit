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

import fr.`override`.linkit.api.connection.ExternalConnection
import org.jetbrains.annotations.NotNull

import java.net.Socket
import scala.util.control.NonFatal

class ServerExternalConnection private(session: ConnectionSession) extends ExternalConnection {

    override val identifier: String = session.server.identifier
    override val

    private val server = session.server
    private val packetTranslator = server.packetTranslator
    private val manager: ConnectionsManager = server.connectionsManager


    @volatile private var closed = false

    override def close(reason: CloseReason): Unit = {
        BusyWorkerPool.checkCurrentIsWorker()
        closed = true
        if (reason.isInternal && isConnected) {
            val sysChannel = session.channel
            sysChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }
        ConnectionPacketWorker.close(reason)

        session.close(reason)

        manager.unregister(identifier)
        workerThread.close()
        ContextLogger.trace(s"Connection closed for $identifier")
    }

    def start(): Unit = {
        if (closed) {
            throw new RelayException("This Connection was already used and is now definitely closed.")
        }
        ConnectionPacketWorker.start()
    }

    def sendPacket(packet: Packet, channelID: Int): Unit = {
        runLater {
            val coords = DedicatedPacketCoordinates(channelID, identifier, server.identifier)
            val result = packetTranslator.fromPacketAndCoords(packet, coords)
            session.send(result)
        }
    }

    def isConnected: Boolean = getState == ConnectionState.CONNECTED

    def getTasksHandler: TasksHandler = session.tasksHandler

    def getConsoleOut: RemoteConsole = session.outConsole

    def getConsoleErr: RemoteConsole = session.errConsole

    def getState: ConnectionState = session.getSocketState

    def runLater(callback: => Unit): Unit = {
        workerThread.runLater(callback)
    }

    override def isClosed: Boolean = closed

    private[server] def updateSocket(socket: Socket): Unit = {
        BusyWorkerPool.checkCurrentIsWorker()
        session.updateSocket(socket)
    }

    def send(result: PacketSerializationResult): Unit = {
        session.send(result)
    }

    private[connection] def send(bytes: Array[Byte]): Unit = {
        session.send(bytes)
    }

    object ConnectionPacketWorker extends PacketWorkerThread {

        @workerExecution
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
                        ServerExternalConnection.this.close(CloseReason.INTERNAL_ERROR)
                    }
            }
        }

        @workerExecution
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
            orderType match {
                case CLIENT_CLOSE => runLater(ServerExternalConnection.this.close(reason))
                case SERVER_CLOSE => server.close(reason)
                case ABORT_TASK => session.tasksHandler.skipCurrent(reason)

                case _ => new UnexpectedPacketException(s"Could not complete order '$orderType', can't be handled by a server or unknown order")
                        .printStackTrace(getConsoleErr)
            }
        }
    }

}

object ServerExternalConnection {

    /**
     * Constructs a ClientConnection without starting it.
     *
     * @throws NullPointerException if the identifier or the socket is null.
     * @return a started ClientConnection.
     * @see [[SocketContainer]]
     * */
    def open(@NotNull session: ConnectionSession): ServerExternalConnection = {
        if (session == null) {
            throw new NullPointerException("Unable to construct ClientConnection : session cant be null")
        }
        val connection = new ServerExternalConnection(session)
        connection.start()
        connection
    }

}
