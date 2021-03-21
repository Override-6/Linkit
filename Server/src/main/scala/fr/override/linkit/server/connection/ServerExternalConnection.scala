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
import fr.`override`.linkit.api.connection.network.ConnectionState
import fr.`override`.linkit.api.connection.packet.serialization.{PacketSerializationResult, PacketTranslator}
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectableFactory, PacketTraffic}
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.connection.task.TasksHandler
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.system.config.ConnectionConfiguration
import fr.`override`.linkit.api.local.system.event.EventNotifier
import fr.`override`.linkit.api.local.system.{JustifiedCloseable, Reason}
import fr.`override`.linkit.core.connection.packet.fundamental.TaskInitPacket
import fr.`override`.linkit.core.connection.packet.traffic.PacketInjections
import fr.`override`.linkit.core.local.concurrency.{BusyWorkerPool, PacketWorkerThread}
import fr.`override`.linkit.core.local.system.{ContextLogger, SystemOrder, SystemPacket}
import org.jetbrains.annotations.NotNull

import java.net.Socket
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class ServerExternalConnection private(session: ConnectionSession,
                                       manager: ExternalConnectionsManager) extends ExternalConnection with JustifiedCloseable {

    private val server = session.server
    override val supportIdentifier: String = server.supportIdentifier

    private val packetTranslator = server.translator
    override val traffic: PacketTraffic = server.traffic
    override val translator: PacketTranslator = server.translator
    override val eventNotifier: EventNotifier = server.eventNotifier
    override val boundIdentifier: String = session.boundIdentifier
    override val configuration: ConnectionConfiguration = session.con

    @volatile private var closed = false

    override def close(reason: Reason): Unit = {
        BusyWorkerPool.checkCurrentIsWorker()
        closed = true
        if (reason.isInternal && isConnected) {
            val sysChannel = session.channel
            sysChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }
        ConnectionPacketWorker.close(reason)

        session.close(reason)

        manager.unregister(supportIdentifier)
        ContextLogger.trace(s"Connection closed for $supportIdentifier")
    }

    override def shutdown(): Unit = ???

    override def isAlive: Boolean = ???

    override def getInjectable[C: ClassTag](injectableID: Int, scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = ???

    override def getState: ConnectionState = session.getSocketState

    override def runLater(callback: => Unit): Unit = {
        server.runLater(callback)
    }

    override def isClosed: Boolean = closed

    def start(): Unit = {
        if (closed) {
            throw ConnectionException("This Connection was already used and is now definitely closed.")
        }
        ConnectionPacketWorker.start()
    }

    def sendPacket(packet: Packet, channelID: Int): Unit = {
        runLater {
            val coords = DedicatedPacketCoordinates(channelID, supportIdentifier, server.supportIdentifier)
            val result = packetTranslator.translate(packet, coords)
            session.send(result)
        }
    }

    def isConnected: Boolean = getState == ConnectionState.CONNECTED

    def getTasksHandler: TasksHandler = session.tasksHandler

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
                    //e.printStackTrace(session.errConsole)
                    runLater {
                        ServerExternalConnection.this.close(Reason.INTERNAL_ERROR)
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
            import SystemOrder._
            orderType match {
                case CLIENT_CLOSE => runLater(ServerExternalConnection.this.close(reason))
                case SERVER_CLOSE => server.shutdown()
                case ABORT_TASK => session.tasksHandler.skipCurrent(reason)

                case _ =>
                    val msg = s"Could not complete order '$orderType', can't be handled by a server or unknown order"
                    ContextLogger.error(msg)
                //UnexpectedPacketException(s"Could not complete order '$orderType', can't be handled by a server or unknown order")
                //.printStackTrace(getConsoleErr)
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
    def open(@NotNull session: ConnectionSession, @NotNull manager: ExternalConnectionsManager): ServerExternalConnection = {
        if (session == null) {
            throw new NullPointerException("Unable to construct ClientConnection : session cant be null")
        }
        val connection = new ServerExternalConnection(session, manager)
        connection.start()
        connection
    }

}
