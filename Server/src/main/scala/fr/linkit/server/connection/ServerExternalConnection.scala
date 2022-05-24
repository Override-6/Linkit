/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.server.connection

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.{ConnectionException, ExternalConnection}
import fr.linkit.api.gnom.network.{ExternalConnectionState, Network}
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.persistence.obj.TrafficObjectReference
import fr.linkit.api.gnom.persistence.{ObjectDeserializationResult, ObjectTransferResult, ObjectTranslator}
import fr.linkit.api.internal.concurrency.{AsyncTask, WorkerPools, workerExecution}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.persistence.SimpleTransferInfo
import org.jetbrains.annotations.NotNull

import java.net.Socket
import java.nio.ByteBuffer

class ServerExternalConnection private(val session: ExternalConnectionSession) extends ExternalConnection {

    import session._

    override val currentIdentifier: String           = server.currentIdentifier
    override val traffic          : PacketTraffic    = server.traffic
    override val translator       : ObjectTranslator = server.translator
    //override val eventNotifier    : EventNotifier    = server.eventNotifier
    override val network          : Network          = session.network
    override val port             : Int              = server.port
    override val boundIdentifier  : String           = session.boundIdentifier
    @volatile private var alive                      = false
    private  val tnol                                = network.gnol.trafficNOL

    override def shutdown(): Unit = {
        WorkerPools.ensureCurrentIsWorker()
        alive = false
        /*if (reason.isInternal && isConnected) {
            val sysChannel = session.channel
            sysChannel.sendOrder(SystemOrder.CLIENT_CLOSE, reason)
        }*/
        readThread.close()
        session.close()

        connectionManager.unregister(currentIdentifier)
        AppLoggers.Connection.info(s"Connection closed for $currentIdentifier")
    }

    override def isAlive: Boolean = alive

    override def getState: ExternalConnectionState = session.getSocketState

    override def runLaterControl[A](@workerExecution task: => A): AsyncTask[A] = {
        server.runLaterControl(task)
    }

    override def runLater(task: => Unit): Unit = server.runLater(task)

    def start(): Unit = {
        if (alive) {
            throw ConnectionException(this, "This Connection was already used and is now definitely closed.")
        }
        alive = true
        readThread.onPacketRead = result => {
            val coordinates: DedicatedPacketCoordinates = result.coords match {
                case d: DedicatedPacketCoordinates => d
                case b: BroadcastPacketCoordinates => b.getDedicated(currentIdentifier)
            }
            val rectifiedResult                         = new ObjectDeserializationResult {
                override val ordinal: Int = result.ordinal
                override def buff: ByteBuffer = result.buff
                override def coords: PacketCoordinates = coordinates
                override def attributes: PacketAttributes = result.attributes
                override def packet: Packet = result.packet
                override def makeDeserialization(): Unit = result.makeDeserialization()
                override def isDeserialized: Boolean = result.isDeserialized
                override def isInjected: Boolean = result.isInjected
                override def informInjected: Unit = result.informInjected
            }
            handlePacket(rectifiedResult)
        }
        readThread.start()
        //Method useless but kept because services could need to be started in the future?
    }

    def sendPacket(packet: Packet, attributes: PacketAttributes, path: Array[Int]): Unit = {
        runLater {
            val coords       = DedicatedPacketCoordinates(path, boundIdentifier, currentIdentifier)
            val config       = traffic.getPersistenceConfig(coords.path)
            val transferInfo = SimpleTransferInfo(coords, attributes, packet, config, network)
            val result       = translator.translate(transferInfo)
            send(result)
        }
    }

    override def isConnected: Boolean = getState == ExternalConnectionState.CONNECTED

    private[connection] def updateSocket(socket: Socket): Unit = {
        WorkerPools.ensureCurrentIsWorker()
        session.updateSocket(socket)
    }

    def canHandlePacketInjection(result: ObjectTransferResult): Boolean = {
        val channelPath = result.coords.path
        channelPath.length == 0 || {
            val reference = new TrafficObjectReference(channelPath)
            val present   = tnol.isPresentOnEngine(boundIdentifier, reference)
            present
        }
    }

    def send(result: ObjectTransferResult): Unit = {
        if (!canHandlePacketInjection(result)) {
            val channelPath = result.coords.path
            val reference   = new TrafficObjectReference(channelPath)
            throw new PacketNotInjectableException(this, s"Engine '$boundIdentifier' does not contains any traffic packet injectable presence at $reference.")
        }
        session.send(result.buff)
    }

    private[connection] def send(buff: ByteBuffer): Unit = {
        session.send(buff)
    }

    @workerExecution
    private def handlePacket(result: ObjectDeserializationResult): Unit = {
        if (alive) {
            serverTraffic.processInjection(result)
        }
    }

    override def getApp: ApplicationContext = server.getApp
}

object ServerExternalConnection {

    /**
     * Constructs a ClientConnection without starting it.
     *
     * @throws NullPointerException if the identifier or the socket is null.
     * @return a started ClientConnection.
     * @see [[SocketContainer]]
     * */
    def open(@NotNull session: ExternalConnectionSession): ServerExternalConnection = {
        if (session == null) {
            throw new NullPointerException("Unable to construct ClientConnection : session cant be null")
        }
        val connection = new ServerExternalConnection(session)
        connection.start()
        connection
    }

}
