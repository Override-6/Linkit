package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.exception.{RelayException, RelayInitialisationException}
import fr.`override`.linkit.api.packet.fundamental.DataPacket
import fr.`override`.linkit.api.packet.traffic.PacketTraffic
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}
import fr.`override`.linkit.server.RelayServer

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
            println(s"Closing '${connection.identifier}'...")
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
        println(s"Registering connection of '$identifier'...")
        if (connections.contains(identifier))
            throw RelayInitialisationException(s"This relay id is already registered ! ('$identifier')")

        if (connections.size > server.configuration.maxConnection)
            throw new RelayException("Maximum connection limit exceeded")

        //Opening ClientConnection and finalizing registration
        val connectionSession = ClientConnectionSession(identifier, socket, server)
        val connection = ClientConnection.open(connectionSession)
        connections.put(identifier, connection)
        connection.sendPacket(DataPacket("OK"), PacketTraffic.SystemChannel)
        connectionSession.initNetwork()

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
        println(s"Connection with $identifier successfully handled !")
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
     * @param bytes the bytes to broadcast
     * @param broadcaster the client who broadcasts the bytes.
     *                    This way, the broadcaster will be ignored
     * */
    def broadcastBytes(bytes: Array[Byte], broadcaster: String): Unit = {
        connections.values
                .filter(con => con.identifier != broadcaster && con.isConnected)
                .foreach(_.sendBytes(bytes))
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
    def getConnection(identifier: String): ClientConnection = connections(identifier)

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
    def containsIdentifier(identifier: String): Boolean = {
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
        connection.sendBytes(bytes)
    }
}