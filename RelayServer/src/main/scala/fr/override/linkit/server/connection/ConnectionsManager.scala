package fr.`override`.linkit.server.connection

import fr.`override`.linkit.skull.Relay
import fr.`override`.linkit.internal.concurrency.PacketWorkerThread
import fr.`override`.linkit.skull.exception.RelayInitialisationException
import fr.`override`.linkit.skull.connection.packet.Packet
import fr.`override`.linkit.skull.internal.system.{CloseReason, JustifiedCloseable, RelayException, RelayInitialisationException}
import fr.`override`.linkit.server.RelayServer
import org.jetbrains.annotations.Nullable

import scala.collection.mutable
import scala.util.control.NonFatal

/**
 * TeamMate of RelayServer, handles the RelayPoint Connections.
 *
 * @see [[RelayServer]]
 * @see [[ClientConnection]]
 * */
class ConnectionsManager(server: RelayServer) extends JustifiedCloseable {

    /**
     * java map containing all RelayPointConnection instances
     * */
    private val connections: mutable.Map[String, ClientConnection] = mutable.Map.empty
    @volatile private var closed = false


    override def close(reason: CloseReason): Unit = {
        for ((_, connection) <- connections) try {
            ContextLogger.trace(s"Closing '${connection.identifier}'...")
            connection.close(reason)
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
        closed = true

    }

    /**
     * creates and register a RelayPoint connection.
     *
     * @param socket the socket to start the connection
     * @throws RelayInitialisationException when a id is already set for this address, or another connection is known under this id.
     * */
    def registerConnection(identifier: String,
                           socket: SocketContainer): Unit = {
        println(s"Registering connection '$identifier' (${socket.remoteSocketAddress()})...")
        if (connections.contains(identifier))
            throw RelayInitialisationException(s"This relay id is already registered ! ('$identifier')")

        if (connections.size > server.configuration.maxConnection)
            throw new RelayException("Maximum connection limit exceeded")

        //Opening ClientConnection and finalizing registration
        val connectionSession = ClientConnectionSession(identifier, socket, server)
        val connection = ClientConnection.open(connectionSession)
        connections.put(identifier, connection)

        println("Sending authorisation packet...")
        server.sendAuthorisedConnection(socket)

        val canConnect = server.securityManager.canConnect(connection)
        if (canConnect) {
            println(s"Connection of '$identifier' was successfully registered !")
            return
        }

        val msg = "Connection rejected by security manager"
        connection.getConsoleErr.println(msg)
        Console.err.println(s"Relay Connection '$identifier': " + msg)

        connections.remove(identifier)
        connection.close(CloseReason.SECURITY_CHECK)
    }

    def broadcastMessage(err: Boolean, msg: String): Unit = {
        connections.values
                .foreach(connection => {
                    if (err)
                        connection.getConsoleErr.println(msg)
                    else connection.getConsoleOut.println(msg)
                })
    }

    /**
     * Broadcast bytes sequence to every connected clients
     * */
    def broadcastBytes(packet: Packet, injectableID: Int, senderID: String, discardedIDs: String*): Unit = {
        PacketWorkerThread.checkNotCurrent()
        val translator = server.packetTranslator
        connections.values
                .filter(con => !discardedIDs.contains(con.identifier) && con.isConnected)
                .foreach(connection => {
                    val coordinates = DedicatedPacketCoordinates(injectableID, connection.identifier, senderID)
                    val result = translator.fromPacketAndCoords(packet, coordinates)
                    connection.send(result)
                })
    }

    /**
     * unregisters a Relay point
     *
     * @param identifier the identifier to disconnect
     * */
    def unregister(identifier: String): Option[ClientConnection] = {
        connections.remove(identifier)
    }


    /**
     * retrieves a RelayPointConnection based on the address
     *
     * @param identifier the identifier linked [[ClientConnection]]
     * @return the found [[ClientConnection]] bound with the identifier
     * */
    @Nullable
    def getConnection(identifier: String): ClientConnection = connections.get(identifier).orNull

    def countConnected: Int = connections.size

    def listIdentifiers: Seq[String] = connections.keys.toSeq

    /**
     * determines if the address is not registered
     *
     * @param identifier the identifier to test
     * @return true if the address is not registered, false instead
     * */
    def isNotRegistered(identifier: String): Boolean = {
        !connections.contains(identifier)
    }

    /**
     * @param identifier the identifier to test
     * @return true if any connected Relay have the specified identifier
     * */
    def isRegistered(identifier: String): Boolean = {
        identifier == server.identifier || connections.contains(identifier) //reserved server identifier
    }

    override def isClosed: Boolean = closed

    /**
     * Deflects a packet to his associated [[ClientConnection]]
     *
     * @throws RelayException if no connection where found for this packet.
     * @param bytes the packet bytes to deflect
     * */
    private[connection] def deflectTo(bytes: Array[Byte], target: String): Unit = {
        val connection = getConnection(target)
        if (connection == null)
            throw new RelayException(s"unknown ID '$target' to deflect packet")
        connection.send(bytes)
    }
}