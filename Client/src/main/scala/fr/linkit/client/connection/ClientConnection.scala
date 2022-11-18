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

package fr.linkit.client.connection

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.{ConnectionInitialisationException, ExternalConnection}
import fr.linkit.api.gnom.network.tag.NameTag
import fr.linkit.api.gnom.network.{ExternalConnectionState, Network}
import fr.linkit.api.gnom.packet._
import fr.linkit.api.gnom.packet.traffic._
import fr.linkit.api.gnom.persistence.{ObjectTranslator, PacketDownload}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.client.ClientApplication
import fr.linkit.client.config.ClientConnectionConfiguration
import fr.linkit.client.connection.traffic.ClientPacketTraffic
import fr.linkit.client.network.ClientSideNetwork
import fr.linkit.engine.gnom.packet.traffic.{DefaultAsyncPacketReader, DynamicSocket}
import fr.linkit.engine.internal.concurrency.PacketReaderThread
import fr.linkit.engine.internal.debug.Debugger
import fr.linkit.engine.internal.system.Rules
import fr.linkit.engine.internal.util.{NumberSerializer, ScalaUtils}
import org.jetbrains.annotations.NotNull

import java.nio.ByteBuffer
import java.util.concurrent.Future
import scala.util.control.NonFatal

class ClientConnection private(session: ClientConnectionSession) extends ExternalConnection {
    Debugger.registerConnection(this)

    import session._


    override val currentName : String                   = configuration.connectionName
    override val port        : Int                      = configuration.remoteAddress.getPort
    override val translator  : ObjectTranslator         = session.translator
    override val boundNT     : NameTag                  = serverNameTag
    override val network     : Network                  = new ClientSideNetwork(this)
    override val traffic     : PacketTraffic            = new ClientPacketTraffic(socket, translator, configuration.defaultPersistenceConfigScript, network, this, appContext)
    private  val packetReader: DefaultAsyncPacketReader = new DefaultAsyncPacketReader(socket, session.appContext, traffic, translator)
    private  val readThread  : PacketReaderThread       = new PacketReaderThread(packetReader, boundNT)


    private var alive = false

    init()

    override def runLater[A](f: => A): Future[A] = session.appContext.runLater(f)

    override def getState: ExternalConnectionState = socket.getState

    override def isAlive: Boolean = alive

    override def isConnected: Boolean = getState == ExternalConnectionState.CONNECTED

    override def shutdown(): Unit = {
        if (!alive)
            return //already shutdown

        readThread.close()
        appContext.unregister(this)

        traffic.close()
        socket.close()

        alive = false
    }

    private def init(): Unit = initReaderThread()

    private def initReaderThread(): Unit = {
        if (alive)
            throw new IllegalStateException(s"Connection already started ! ($currentName)")
        alive = true

        socket.addConnectionStateListener(tryReconnect)
        readThread.onPacketRead = result => {
            try {
                val coordinates: DedicatedPacketCoordinates = result.coords match {
                    case dedicated: DedicatedPacketCoordinates => dedicated
                    case _: BroadcastPacketCoordinates         => throw new UnsupportedOperationException("Broadcast unsupported until supported by persistence system.")
                    case null                                  => throw new PacketException("Received null packet coordinates.")
                    case other                                 => throw new PacketException(s"Unknown packet coordinates of type ${other.getClass.getName}. Only Dedicated and Broadcast packet coordinates are allowed on this client.")
                }
                handlePacket(result, coordinates)
            } catch {
                case NonFatal(e) =>
                    val coords = result.coords
                    val errMsg = s"Could not deserialize packet ${coords.path.mkString("/")}$$${coords.senderTag}:${result.ordinal}."
                    if (AppLoggers.Persistence.isDebugEnabled) {
                        AppLoggers.Persistence.error(errMsg)
                        e.printStackTrace()
                    } else {
                        val st                         = e.getStackTrace
                        val firstInterestingStackTrace = st.find(el => Class.forName(el.getClassName).getClassLoader != null).getOrElse(st.head)
                        AppLoggers.Persistence.error(s"$errMsg $e ($firstInterestingStackTrace)")
                    }
            }
        }
        readThread.onReadException = () => this.runLater(this.shutdown())
        readThread.start()
    }

    private def tryReconnect(state: ExternalConnectionState): Unit = {
        //TODO -------------------- AUTOMATIC RECONNECTION MAINTAINED --------------------
        /*val bytes         = currentName.getBytes()
        val welcomePacket = NumberSerializer.serializeInt(bytes.length) ++ bytes

        if (state == ExternalConnectionState.CONNECTED && socket.isOpen) runLater {
            if (isAlive) {
                socket.write(welcomePacket) //The welcome packet will let the server continue its socket handling
                systemChannel.nextPacket[BooleanPacket]
            }
        }*/
    }

    private def handlePacket(result: PacketDownload, coordinates: DedicatedPacketCoordinates): Unit = {
        //FIXME Ugly
        val rectifiedResult = new PacketDownload {
            override val ordinal: Int        = result.ordinal
            override val buff   : ByteBuffer = result.buff

            override def makeDeserialization(): Unit = result.makeDeserialization()

            override def isDeserialized: Boolean = result.isDeserialized

            override def coords: PacketCoordinates = coordinates

            override def attributes: PacketAttributes = result.attributes

            override def packet: Packet = result.packet

            override def isInjected: Boolean = result.isInjected

            override def informInjected: Unit = result.informInjected
        }
        traffic.processInjection(rectifiedResult)
    }

    override def getApp: ApplicationContext = appContext

}

object ClientConnection {

    @throws[ConnectionInitialisationException]("If Something went wrong during the initialization.")
    @NotNull
    def open(socket       : ClientDynamicSocket,
             context      : ClientApplication,
             configuration: ClientConnectionConfiguration): ClientConnection = {

        //Initializing values that will be used for packet transactions during the initialization.
        val translator = configuration.translatorFactory(context)

        //WelcomePacket informational fields
        val identifier = configuration.connectionName

        //Aliases
        val IDbytes   = identifier.getBytes()
        val separator = Rules.WPArgsSeparator

        //Creating and sending welcomePacket
        val bytes         = IDbytes ++ separator
        val welcomePacket = NumberSerializer.serializeInt(bytes.length) ++ bytes
        socket.write(welcomePacket)

        //Ensuring that the server has accepted this connection based on the previously sent welcome packet.
        assertAccepted(socket, socket.read(1))

        //The contains the server identifier bytes' string
        val buff       = socket.read(socket.readInt())
        val serverName = new String(buff)
        socket.identifier = serverName

        //Constructing connection instance...
        val session    = ClientConnectionSession(socket, context, configuration, NameTag(serverName), translator)
        val connection = new ClientConnection(session)
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
