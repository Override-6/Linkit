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

package fr.linkit.server.connection

import fr.linkit.api.application.ApplicationContext
import fr.linkit.api.application.connection.CentralConnection
import fr.linkit.api.gnom.network.Network
import fr.linkit.api.gnom.packet.traffic.PacketTraffic
import fr.linkit.api.gnom.persistence.ObjectTranslator
import fr.linkit.api.internal.concurrency.WorkerPool
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.packet.traffic.DynamicSocket
import fr.linkit.engine.internal.concurrency.VirtualProcrastinator
import fr.linkit.engine.internal.debug.Debugger
import fr.linkit.engine.internal.system.Rules
import fr.linkit.engine.internal.util.NumberSerializer.serializeInt
import fr.linkit.server.config.ServerConnectionConfiguration
import fr.linkit.server.connection.traffic.ServerPacketTraffic
import fr.linkit.server.network.ServerSideNetwork
import fr.linkit.server.{ServerApplication, ServerException}
import org.jetbrains.annotations.Nullable

import java.net.{ServerSocket, SocketException}
import java.util.concurrent.Future
import scala.util.control.NonFatal

class ServerConnection(applicationContext: ServerApplication,
                       val configuration : ServerConnectionConfiguration) extends CentralConnection {

    Debugger.registerConnection(this)

    override val currentIdentifier : String                     = configuration.identifier
    override val translator        : ObjectTranslator           = configuration.translatorFactory(applicationContext)
    override val port              : Int                        = configuration.port
    private  val workerPool        : WorkerPool                 = VirtualProcrastinator(currentIdentifier)
    private  val serverSocket      : ServerSocket               = new ServerSocket(configuration.port)
    private  val connectionsManager: ExternalConnectionsManager = new ExternalConnectionsManager(this)
    private  val serverTraffic     : ServerPacketTraffic        = new ServerPacketTraffic(this, configuration.defaultPersistenceConfigScript)
    override val traffic           : PacketTraffic              = serverTraffic
    private  val sideNetwork       : ServerSideNetwork          = new ServerSideNetwork(serverTraffic)
    override val network           : Network                    = sideNetwork
    @volatile private var alive    : Boolean                    = false

    override def getApp: ApplicationContext = applicationContext

    override def shutdown(): Unit = {
        if (!alive)
            return
        alive = false

        val port = configuration.port

        AppLoggers.Connection.info(s"Server '$currentIdentifier' on port $port prepares to shutdown...")
        applicationContext.unregister(this)

        connectionsManager.close()
        AppLoggers.Connection.info(s"Server '$currentIdentifier' shutdown.")
    }

    def start(): Unit = {
        if (alive)
            throw new ServerException(this, "Server is already started.")
        AppLoggers.Connection.info(s"Server '$currentIdentifier' starts on port ${configuration.port}")
        AppLoggers.Connection.debug(s"Identifier Ambiguity Strategy : ${configuration.identifierAmbiguityStrategy}")
        sideNetwork.initialize()

        try {
            loadSocketListener()
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                shutdown()
        }
    }

    override def isAlive: Boolean = alive

    override def getConnection(identifier: String): Option[ServerExternalConnection] = Option(connectionsManager.getConnection(identifier))

    override def countConnections: Int = connectionsManager.countConnections


    override def runLater[A](f: => A): Future[A] = workerPool.runLater(f)

    private[connection] def getSideNetwork: ServerSideNetwork = sideNetwork


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////  C L I E N T  I N I T I A L I S A T I O N  H A N D L I N G  /////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //TODO Documentation

    private def listenSocketConnection(): Unit = {
        val socketContainer = new SocketContainer(true)
        val port            = configuration.port
        AppLoggers.Connection.debug(s"Ready to accept next connection on port $port")

        try {
            val socket = serverSocket.accept()
            if (!alive)
                return
            socketContainer.set(socket)
        } catch {
            case e: SocketException =>
                val msg = e.getMessage.toLowerCase
                if (msg == "socket closed" || msg == "socket is closed")
                    return
                Console.err.println(msg)
                onException(e)
            case NonFatal(e)        =>
                e.printStackTrace()
                onException(e)
        }

        AppLoggers.Connection.info(s"Socket accepted (${socketContainer.getCurrent})")
        runLater {
            AppLoggers.Connection.debug(s"Handling client socket ${socketContainer.getCurrent}...")
            handleSocket(socketContainer)
        }

        def onException(e: Throwable): Unit = runLater {
            sendRefusedConnection(socketContainer, s"An exception occurred in server during client connection initialisation ($e)") //sends a negative response for the fr.linkit.client initialisation handling
            //shutdown()
        }
    }

    /**
     * Reads a welcome packet from a connection.<br>
     * The Welcome packet is the first packet that a connection must send
     * in order to provide its identifier to the server and its hasher & translator signature.
     *
     * @return the identifier bound with the socket
     * */
    private def readWelcomePacket(socket: SocketContainer): String = {
        val welcomePacketLength = socket.readInt()
        val welcomePacket       = socket.read(welcomePacketLength)
        new String(welcomePacket)
    }

    /**
     * @return a [[WelcomePacketVerdict]] if the scan that decides if the connection that sends
     *         this welcomePacket should be discarded by the server.
     * */
    private def scanWelcomePacket(welcomePacket: String): WelcomePacketVerdict = {
        val args = welcomePacket.split(";")
        if (args.length != Rules.WPArgsLength)
            return WelcomePacketVerdict(null, false, s"Arguments length does not conform to server's rules of ${Rules.WPArgsLength}")
        try {
            val identifier = args(0)

            /*if (!(configuration.hasher.signature sameElements hasherSignature))
                return WelcomePacketVerdict(identifier, false, "Hasher signatures mismatches !")*/

            if (!Rules.IdentifierPattern.matcher(identifier).matches())
                return WelcomePacketVerdict(identifier, false, "Provided identifier does not matches server's rules.")

            WelcomePacketVerdict(identifier, true)
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                WelcomePacketVerdict(null, false, e.getMessage)
        }
    }

    private def handleSocket(socket: SocketContainer): Unit = {
        val welcomePacket = readWelcomePacket(socket)
        val verdict       = scanWelcomePacket(welcomePacket)
        if (!verdict.accepted) {
            verdict.concludeRefusal(socket)
            return
        }
        val identifier = verdict.identifier
        socket.identifier = identifier
        handleNewConnection(identifier, socket)
    }

    private def loadSocketListener(): Unit = {
        val thread = new Thread(() => {
            alive = true
            while (alive) listenSocketConnection()
        })
        thread.setName(s"Socket Listener : ${configuration.port}")
        thread.start()
    }

    private def handleNewConnection(identifier: String,
                                    socket    : SocketContainer): Unit = {

        val currentConnection = getConnection(identifier)
        //There is no currently connected connection with the same identifier on this network.
        if (currentConnection.isEmpty) {
            connectionsManager.registerConnection(identifier, socket)
            return
        }

        handleConnectionIdAmbiguity(currentConnection.get, socket)
    }

    private def handleConnectionIdAmbiguity(conflicted: ServerExternalConnection,
                                            socket    : SocketContainer): Unit = {
        val identifier = conflicted.boundIdentifier
        if (!conflicted.isConnected) {
            conflicted.updateSocket(socket.getCurrent)
            sendAuthorisedConnection(socket)
            AppLoggers.Connection.info(s"The connection with '${conflicted.boundIdentifier}' has been resumed.")
            return
        }
        val strategy = configuration.identifierAmbiguityStrategy
        AppLoggers.Connection.warn(s"Connection '$identifier' conflicts with socket $socket. Applying Ambiguity Strategy '$strategy'...")

        val rejectMsg = s"Another engine with id '$identifier' is currently connected on the targeted network."
        import fr.linkit.server.config.AmbiguityStrategy._
        configuration.identifierAmbiguityStrategy match {
            case CLOSE_SERVER =>
                sendRefusedConnection(socket, rejectMsg + " Consequences: Closing Server...")
                shutdown()

            case REJECT_NEW =>
                Console.err.println(s"Rejected connection of a client because it gave an invalid identifier (identifier '${identifier}' is already registered).")
                sendRefusedConnection(socket, rejectMsg)

            case REPLACE =>
                connectionsManager.unregister(identifier).get.shutdown()
                connectionsManager.registerConnection(identifier, socket)
            //The connection initialisation packet isn't sent here because it is send into the registerConnection method.

            case DISCONNECT_BOTH =>
                connectionsManager.unregister(identifier).get.shutdown()
                sendRefusedConnection(socket, rejectMsg + " Consequences : Disconnected both")
        }
    }

    private[connection] def sendAuthorisedConnection(socket: DynamicSocket): Unit = {
        socket.write(Array(Rules.ConnectionAccepted))
        val bytes = currentIdentifier.getBytes()
        socket.write(serializeInt(bytes.length) ++ bytes)
    }

    private[connection] def sendRefusedConnection(socket: DynamicSocket, message: String): Unit = {
        socket.write(Array(Rules.ConnectionRefused))
        val bytes = message.getBytes()
        socket.write(serializeInt(bytes.length) ++ bytes)
    }

    private case class WelcomePacketVerdict(@Nullable("bad-packet-format") identifier: String,
                                            accepted                                 : Boolean,
                                            @Nullable("accepted=true") refusalMessage: String = null) {

        def concludeRefusal(socket: DynamicSocket): Unit = {
            if (identifier == null) {
                AppLoggers.Connection.error(s"An unknown connection have been discarded: $refusalMessage")
            } else {
                AppLoggers.Connection.error(s"Connection $identifier has been discarded: $refusalMessage")
            }
            sendRefusedConnection(socket, s"Connection discarded by the server: $refusalMessage")
        }
    }

}
