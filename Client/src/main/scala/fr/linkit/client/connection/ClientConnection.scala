/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.client.connection

import fr.linkit.api.connection.network.{ExternalConnectionState, Network}
import fr.linkit.api.connection.packet._
import fr.linkit.api.connection.packet.channel.ChannelScope
import fr.linkit.api.connection.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.persistence.PacketTranslator
import fr.linkit.api.connection.packet.persistence.context.PersistenceConfig
import fr.linkit.api.connection.packet.traffic._
import fr.linkit.api.connection.{ConnectionInitialisationException, ExternalConnection}
import fr.linkit.api.local.ApplicationContext
import fr.linkit.api.local.concurrency.{AsyncTask, WorkerPools, packetWorkerExecution, workerExecution}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.local.system.event.EventNotifier
import fr.linkit.client.ClientApplication
import fr.linkit.client.connection.network.ClientSideNetwork
import fr.linkit.client.local.config.ClientConnectionConfiguration
import fr.linkit.engine.connection.packet.fundamental.ValPacket.BooleanPacket
import fr.linkit.engine.connection.packet.traffic.DynamicSocket
import fr.linkit.engine.local.system.Rules
import fr.linkit.engine.local.utils.{NumberSerializer, ScalaUtils}
import org.jetbrains.annotations.NotNull

import scala.reflect.ClassTag
import scala.util.control.NonFatal

class ClientConnection private(session: ClientConnectionSession) extends ExternalConnection {

    import session._

    init()

    override val currentIdentifier       : String            = configuration.identifier
    override val port                    : Int               = configuration.remoteAddress.getPort
    override val translator              : PacketTranslator  = session.translator
    override val eventNotifier           : EventNotifier     = session.eventNotifier
    override val traffic                 : PacketTraffic     = session.traffic
    override val boundIdentifier         : String            = serverIdentifier
    override val defaultPersistenceConfig: PersistenceConfig = traffic.defaultPersistenceConfig
    private  val sideNetwork             : ClientSideNetwork = new ClientSideNetwork(this, defaultPersistenceConfig.getReferenceStore)
    override val network                 : Network           = sideNetwork
    @volatile private var alive                              = true

    override def getInjectable[C <: PacketInjectable : ClassTag](injectableID: Int, config: PersistenceConfig, factory: PacketInjectableFactory[C], scopeFactory: ScopeFactory[_ <: ChannelScope]): C = {
        traffic.getInjectable(injectableID, config, factory, scopeFactory)
    }

    override def findStore(id: Int): Option[PacketInjectableStore] = traffic.findStore(id)

    override def createStore(id: Int, config: PersistenceConfig): PacketInjectableStore = traffic.createStore(id, config)

    override def runLater(@workerExecution task: => Unit): Unit = appContext.runLater(task)

    override def runLaterControl[A](task: => A): AsyncTask[A] = appContext.runLaterControl(task)

    override def getState: ExternalConnectionState = socket.getState

    override def isAlive: Boolean = alive

    override def isConnected: Boolean = getState == ExternalConnectionState.CONNECTED

    @workerExecution
    override def shutdown(): Unit = {
        WorkerPools.ensureCurrentIsWorker("Shutdown must be performed in a contextual thread pool.")
        if (!alive)
            return //already shutdown

        readThread.close()
        appContext.unregister(this)

        traffic.close()
        socket.close()

        alive = false
    }

    @workerExecution
    private def init(): Unit = {
        session.traffic.setConnection(this)
        initPacketReader()
    }

    @workerExecution
    private def initPacketReader(): Unit = {
        WorkerPools.ensureCurrentIsWorker("Can't start in a non worker pool !")
        if (alive)
            throw new IllegalStateException(s"Connection already started ! ($currentIdentifier)")
        alive = true

        socket.addConnectionStateListener(tryReconnect)
        readThread.onPacketRead = result => {
            try {
                val coordinates: DedicatedPacketCoordinates = result.coords match {
                    case dedicated: DedicatedPacketCoordinates => dedicated
                    case broadcast: BroadcastPacketCoordinates => broadcast.getDedicated(currentIdentifier)
                    case null                                  => throw new PacketException("Received null packet coordinates.")
                    case other                                 => throw new PacketException(s"Unknown packet coordinates of type ${other.getClass.getName}. Only Dedicated and Broadcast packet coordinates are allowed on this client.")
                }
                handlePacket(result.packet, result.attributes, coordinates)
            } catch {
                case NonFatal(e) => throw new PacketException(s"Could not deserialize '${ScalaUtils.toPresentableString(result.buff)}'", e)
            }
        }
        readThread.start()
    }

    @packetWorkerExecution //So the runLater must be specified in order to perform network operations
    private def tryReconnect(state: ExternalConnectionState): Unit = {
        val bytes         = currentIdentifier.getBytes()
        val welcomePacket = NumberSerializer.serializeInt(bytes.length) ++ bytes

        if (state == ExternalConnectionState.CONNECTED && socket.isOpen) runLater {
            socket.write(welcomePacket) //The welcome packet will let the server continue its socket handling
            systemChannel.nextPacket[BooleanPacket]
            sideNetwork.connectionEngine.update()
            translator.initNetwork(network)
        }
    }

    private def handlePacket(packet: Packet, attributes: PacketAttributes, coordinates: DedicatedPacketCoordinates): Unit = {
        //packet match {
            //case system: SystemPacket => handleSystemPacket(system, coordinates)
        //    case _: Packet =>
                //println(s"START OF INJECTION ($packet, $coordinates, $number) - ${Thread.currentThread()}")
                traffic.processInjection(packet, attributes, coordinates)
            //println(s"ENT OF INJECTION ($packet, $coordinates, $number) - ${Thread.currentThread()}")
       // }
    }

    override def getApp: ApplicationContext = appContext
}

object ClientConnection {

    @throws[ConnectionInitialisationException]("If Something went wrong during the initialization.")
    @NotNull
    def open(socket: ClientDynamicSocket,
             context: ClientApplication,
             configuration: ClientConnectionConfiguration): ClientConnection = {

        //Initializing values that will be used for packet transactions during the initialization.
        val translator = configuration.translatorFactory(context)

        //WelcomePacket informational fields
        val identifier          = configuration.identifier
        val translatorSignature = translator.signature
        val hasherSignature     = configuration.hasher.signature

        //Aliases
        val IDbytes   = identifier.getBytes()
        val separator = Rules.WPArgsSeparator

        //Creating and sending welcomePacket
        val bytes         = IDbytes ++ separator ++ translatorSignature ++ separator ++ hasherSignature
        val welcomePacket = NumberSerializer.serializeInt(bytes.length) ++ bytes
        socket.write(welcomePacket)

        //Ensuring that the server has accepted this connection based on the previously sent welcome packet.
        assertAccepted(socket, socket.read(1))

        //The contains the server identifier bytes' string
        val buff             = socket.read(socket.readInt())
        val serverIdentifier = new String(buff)
        socket.identifier = serverIdentifier

        //Constructing connection instance session
        AppLogger.info(s"${identifier}: Stage 1 completed : Connection seems able to support this server configuration.")
        val sessionInfo = ClientConnectionSessionInfo(context, configuration, serverIdentifier, translator)
        val session     = ClientConnectionSession(socket, sessionInfo)
        //Constructing connection instance...
        //Stage 2 will be completed into ClientConnection constructor.
        val connection  = new ClientConnection(session)
        AppLogger.info(s"$identifier: Stage 3 completed : ClientSideNetwork and Connection instances created.")
        connection
    }

    /*
    * Reading Welcome Packet
    * */
    private def assertAccepted(socket: DynamicSocket, buff: Array[Byte]): Unit = {
        val header = buff.head
        if (header != Rules.ConnectionAccepted && header != Rules.ConnectionRefused)
            throw new ConnectionInitialisationException(s"Received unexpected welcome packet verdict format (received: ${ScalaUtils.toPresentableString(buff)}")

        val isAccepted = header == Rules.ConnectionAccepted
        if (!isAccepted) {
            val buff       = socket.read(socket.readInt())
            val msg        = ScalaUtils.toPresentableString(buff)
            val serverPort = socket.remoteSocketAddress().getPort
            throw new ConnectionInitialisationException(s"Server (port: $serverPort) refused connection: $msg")
        }
    }

}
