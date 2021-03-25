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

package fr.`override`.linkit.client.connection

import fr.`override`.linkit.api.connection.network.{ExternalConnectionState, Network}
import fr.`override`.linkit.api.connection.packet.serialization.{PacketDeserializationResult, PacketTranslator}
import fr.`override`.linkit.api.connection.packet.traffic.ChannelScope.ScopeFactory
import fr.`override`.linkit.api.connection.packet.traffic.PacketTraffic.SystemChannelID
import fr.`override`.linkit.api.connection.packet.traffic._
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.connection.{ConnectionInitialisationException, ExternalConnection}
import fr.`override`.linkit.api.local.concurrency.{packetWorkerExecution, workerExecution}
import fr.`override`.linkit.api.local.system.event.EventNotifier
import fr.`override`.linkit.api.local.system.security.BytesHasher
import fr.`override`.linkit.client.ClientApplication
import fr.`override`.linkit.client.config.ClientConnectionConfiguration
import fr.`override`.linkit.core.connection.packet.fundamental.ValPacket.BooleanPacket
import fr.`override`.linkit.core.connection.packet.serialization.NumberSerializer
import fr.`override`.linkit.core.connection.packet.traffic.{DefaultPacketReader, DynamicSocket, PacketInjections}
import fr.`override`.linkit.core.local.concurrency.{BusyWorkerPool, PacketReaderThread}
import fr.`override`.linkit.core.local.system.{ContextLogger, Rules, SystemPacket, SystemPacketChannel}
import org.jetbrains.annotations.NotNull

import scala.reflect.ClassTag

class ClientConnection private(session: ClientConnectionSession) extends ExternalConnection {

    import session._

    start() //Weird

    override val supportIdentifier: String = configuration.identifier
    override val boundIdentifier: String = serverIdentifier
    override val translator: PacketTranslator = configuration.translator
    override val traffic: PacketTraffic = session.traffic
    override val network: Network = session.initNetwork(this)
    override val eventNotifier: EventNotifier = session.eventNotifier

    private val systemChannel: SystemPacketChannel = new SystemPacketChannel(ChannelScope.reserved(serverIdentifier)(traffic.newWriter(SystemChannelID)))
    @volatile private var alive = true

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        traffic.getInjectable(injectableID, scopeFactory, factory)
    }

    override def runLater(@workerExecution task: => Unit): Unit = appContext.runLater(task)

    override def getState: ExternalConnectionState = socket.getState

    override def isAlive: Boolean = alive

    @workerExecution
    override def shutdown(): Unit = {
        BusyWorkerPool.checkCurrentIsWorker("Shutdown must be performed in a contextual thread pool.")
        if (!alive)
            return //already shutdown

        readThread.close()
        appContext.unregister(this)

        traffic.close()
        socket.close()

        alive = false
    }

    @workerExecution
    def start(): Unit = {
        BusyWorkerPool.checkCurrentIsWorker("Can't start in a non worker pool !")
        if (alive)
            throw new IllegalStateException(s"Connection already started ! ($supportIdentifier)")
        alive = true

        socket.addConnectionStateListener(tryReconnect)
        readThread.onPacketRead = (result, packetNumber) => {
            val coordinates: DedicatedPacketCoordinates = result.coords match {
                case d: DedicatedPacketCoordinates => d
                case _ => throw new IllegalArgumentException("Packet must be dedicated to this connection.")
            }
            handlePacket(result.packet, coordinates, packetNumber)
        }
    }

    @packetWorkerExecution //So the runLater must be specified in order to perform network operations
    private def tryReconnect(state: ExternalConnectionState): Unit = {
        val bytes = supportIdentifier.getBytes()
        val welcomePacket = NumberSerializer.serializeInt(bytes.length) ++ bytes

        if (state == ExternalConnectionState.CONNECTED && socket.isOpen) runLater {
            socket.write(welcomePacket) //The welcome packet will let the server continue his socket handling
            systemChannel.nextPacket[BooleanPacket]
            session.network.update()
            translator.updateCache(network.globalCache)
        }
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

}

object ClientConnection {


    @throws[ConnectionInitialisationException]("If Something went wrong during the initialization.")
    @NotNull
    def open(socket: ClientDynamicSocket, context: ClientApplication, configuration: ClientConnectionConfiguration): ClientConnection = {

        //Initializing values that will be used for packet transactions during the initialization.
        val translator = configuration.translator
        val packetReader = new DefaultPacketReader(socket, BytesHasher.inactive, translator)

        //WelcomePacket informational fields
        val identifier = configuration.identifier
        val translatorSignature = translator.signature
        val hasherSignature = configuration.hasher.signature

        //Aliases
        val IDbytes = identifier.getBytes()
        val separator = Rules.WPArgsSeparator

        //Creating and sending welcomePacket
        val bytes = IDbytes ++ separator ++ translatorSignature ++ separator ++ hasherSignature
        val welcomePacket = NumberSerializer.serializeInt(bytes.length) ++ bytes
        socket.write(welcomePacket)

        //Ensuring that the server has accepted this connection's signatures based on the previously sent welcome packet.
        packetReader.nextPacket(assertAccepted)

        //Instantiating connection instances...
        var connection: ClientConnection = null

        //Server should send a packet which provides its identifier.
        //This packet concludes phase 1 of connection's initialization.
        packetReader.nextPacket((result, _) => {
            //The contains the server identifier bytes' string
            val serverIdentifier = new String(result.bytes)
            socket.identifier = serverIdentifier

            ContextLogger.info(s"${identifier}: Stage 1 completed : Connection seems able to support this server configuration.")
            val readThread = new PacketReaderThread(packetReader, context, serverIdentifier)
            val sessionInfo = ClientConnectionSessionInfo(context, configuration, readThread)
            val session = ClientConnectionSession(socket, sessionInfo, serverIdentifier)
            ContextLogger.info(s"$identifier: Stage 3 completed : ClientSideNetwork and Connection instances created.")

            //Concluding instance...
            connection = new ClientConnection(session)
        })

        //The server couldn't send the packet.
        if (connection == null)
            throw new ConnectionInitialisationException("Couldn't receive server identifier's packet. The connection have no choice to abort this initialization.")
        //return the connection instance
        connection
    }

    private def assertAccepted(result: PacketDeserializationResult, ignored: Int)(socket: DynamicSocket, reader: PacketReader): Unit = {
        val header = result.bytes(0)
        if (header != Rules.ConnectionAccepted && header != Rules.ConnectionRefused)
            throw new ConnectionInitialisationException("Received unexpected welcome packet verdict format")
        val isAccepted = header == Rules.ConnectionAccepted
        if (!isAccepted) {
            reader.nextPacket((result, _) => {
                val msg = new String(result.bytes)
                val serverPort = socket.remoteSocketAddress().getPort
                throw new ConnectionInitialisationException(s"Server (port: $serverPort) refused connection: $msg")
            })
        }
    }

}
