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

import fr.`override`.linkit.api.connection.ExternalConnection
import fr.`override`.linkit.api.connection.network.{ConnectionState, Network}
import fr.`override`.linkit.api.connection.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectable, PacketInjectableFactory, PacketTraffic}
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.system.config.ConnectionConfiguration
import fr.`override`.linkit.api.local.system.event.EventNotifier
import fr.`override`.linkit.client.network.PointNetwork
import fr.`override`.linkit.core.connection.network.cache.AbstractSharedCacheManager
import fr.`override`.linkit.core.connection.packet.UnexpectedPacketException
import fr.`override`.linkit.core.connection.packet.serialization.CompactedPacketTranslator
import fr.`override`.linkit.core.connection.packet.traffic.{DynamicSocket, PacketInjections, PacketReader, SocketPacketTraffic}
import fr.`override`.linkit.core.local.concurrency.{BusyWorkerPool, PacketWorkerThread}
import fr.`override`.linkit.core.local.system.event.DefaultEventNotifier
import fr.`override`.linkit.core.local.system.{ContextLogger, SystemPacket}

import java.nio.channels.AsynchronousCloseException
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class ClientConnection(socket: DynamicSocket,
                       appContext: ClientApplicationContext,
                       serverIdentifier: String,
                       override val configuration: ConnectionConfiguration) extends ExternalConnection {

    override val supportIdentifier: String = configuration.identifier
    override val boundIdentifier: String = serverIdentifier
    override val translator: PacketTranslator = new CompactedPacketTranslator(supportIdentifier, configuration.hasher)
    override val traffic: PacketTraffic = new SocketPacketTraffic(socket, translator, supportIdentifier)
    override val network: Network = initNetwork
    override val eventNotifier: EventNotifier = new DefaultEventNotifier

    @volatile private var alive = true

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ChannelScope.ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        traffic.getInjectable(injectableID, scopeFactory, factory)
    }

    override def runLater(@workerExecution task: => Unit): Unit = appContext.runLater(task)

    override def getState: ConnectionState = socket.getState

    override def isAlive: Boolean = alive

    @workerExecution
    override def shutdown(): Unit = {
        BusyWorkerPool.checkCurrentIsWorker("Shutdown must be performed in a contextual thread pool.")
        if (!alive)
            return //already shutdown

        PointPacketWorkerThread.close()
        appContext.unregister(this)

        traffic.close()
        socket.close()

        alive = false

    }

    private def checkCoordinates(coordinates: DedicatedPacketCoordinates): Unit = {
        val targetID = coordinates.targetID
        if (targetID != supportIdentifier)
            throw UnexpectedPacketException(s"Could not handle received packet, coordinates aren't targeting this connection !")
    }

    private def initNetwork: Network = {
        if (network != null)
            throw new IllegalStateException("Network is already initialized !")
        val globalCache = AbstractSharedCacheManager.get(supportIdentifier, supportIdentifier)(traffic)
        new PointNetwork(this, globalCache)
    }

    private def handleSystemPacket(system: SystemPacket, coords: DedicatedPacketCoordinates): Unit = {
        val order = system.order
        val reason = system.reason.reversedPOV()
        val sender = coords.senderID

        import fr.`override`.linkit.core.local.system.SystemOrder._
        order match {
            case CLIENT_CLOSE => shutdown()
            //FIXME case ABORT_TASK => tasksHandler.skipCurrent(reason)

            //FIXME weird use of exceptions/remote print
            case SERVER_CLOSE =>
            //FIXME UnexpectedPacketException(s"System packet order '$order' couldn't be handled by this RelayPoint : Received forbidden order")
            //        .printStackTrace(getConsoleErr(sender))

            case _ => //FIXME UnexpectedPacketException(s"System packet order '$order' couldn't be handled by this RelayPoint : Unknown order")
            // .printStackTrace(getConsoleErr(sender))
        }
    }

    private def handlePacket(packet: Packet, coordinates: DedicatedPacketCoordinates, number: Int): Unit = {
        packet match {
            //FIXME case init: TaskInitPacket => tasksHandler.handlePacket(init, coordinates)
            case system: SystemPacket => handleSystemPacket(system, coordinates)
            case _: Packet =>
                val injection = PacketInjections.createInjection(packet, coordinates, number)
                //println(s"START OF INJECTION ($packet, $coordinates, $number) - ${Thread.currentThread()}")
                traffic.handleInjection(injection)
            //println(s"ENT OF INJECTION ($packet, $coordinates, $number) - ${Thread.currentThread()}")
        }
    }

    private object PointPacketWorkerThread extends PacketWorkerThread() {

        private val packetReader = new PacketReader(socket, configuration.hasher)
        @volatile private var packetsReceived = 0

        override protected def refresh(): Unit = {
            try {
                listen()
            } catch {
                case _: AsynchronousCloseException =>
                    onException("Asynchronous close.")

                case NonFatal(e) =>
                    e.printStackTrace()
                    onException(s"Suddenly disconnected from the server.")
            }

            def onException(msg: String): Unit = {
                ContextLogger.warn(msg)

                runLater {
                    shutdown()
                }
            }
        }

        private def listen(): Unit = {
            val bytes = packetReader.readNextPacketBytes()
            if (bytes == null)
                return
            //NETWORK-DEBUG-MARK
            val preview = new String(bytes.take(1000)).replace('\n', ' ').replace('\r', ' ')
            println(s"${Console.YELLOW}received : $preview (l: ${bytes.length})${Console.RESET}")
            val packetNumber = packetsReceived + 1
            packetsReceived += 1

            runLater { //handles and deserializes the packet in the worker thread pool

                val (packet, coordinates) = translator.translate(bytes)

                //println(s"RECEIVED PACKET $packet WITH COORDINATES $coordinates. This packet will be handled in thread ${Thread.currentThread()}")

                coordinates match {
                    case dedicated: DedicatedPacketCoordinates =>
                        checkCoordinates(dedicated)
                        handlePacket(packet, dedicated, packetNumber)
                    case other => throw UnexpectedPacketException(s"Only DedicatedPacketCoordinates can be handled by a RelayPoint. Received : ${other.getClass.getName}")
                }
            }
        }

    }

}
