package fr.overridescala.vps.ftp.server.connection

import java.io.Closeable
import java.net.{Socket, SocketAddress}

import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.server.RelayServer

import scala.collection.mutable

/**
 * TeamMate of RelayServer, handles the RelayPoint Connections.
 *
 * @see [[RelayServer]]
 * @see [[ClientConnectionThread]]
 * */
class ConnectionsManager(server: RelayServer) extends Closeable {

    /**
     * java map containing all RelayPointConnection instances
     * */
    private val connections: mutable.Map[SocketAddress, ClientConnectionThread] = mutable.Map.empty

    override def close(): Unit = {
        for ((_, connection) <- connections)
            connection.close()
    }

    /**
     * creates and register a RelayPoint connection.
     *
     * @param socket the socket to start the connection
     * @throws RelayInitialisationException when a id is already set for this address, or another connection is known under this id.
     * */
    def register(socket: Socket): Unit = {
        val address = socket.getRemoteSocketAddress
        checkAddress(address)
        val connection = new ClientConnectionThread(socket, server)
        connection.start()
        connections.put(address, connection)
    }

    /**
     * disconnects a Relay point
     *
     * @param address the address to disconnect
     * */
    def disconnect(address: SocketAddress): Unit = {
        connections(address).close()
        connections.remove(address)
    }

    /**
     * get a relay from
     * */
    def getConnectionFromAddress(address: SocketAddress): ClientConnectionThread = {
        if (!connections.contains(address))
            return null
        connections(address)
    }

    /**
     * retrieves a RelayPointConnection based on the address
     *
     * @param identifier the identifier linked [[ClientConnectionThread]]
     * @return the found [[ClientConnectionThread]] bound with the identifier
     * */
    def getConnectionFromIdentifier(identifier: String): ClientConnectionThread = {
        for ((_, connection) <- connections) {
            if (connection.tasksHandler.identifier.equals(identifier))
                return connection
        }
        null
    }

    /**
     * determines if the address is not registered
     *
     * @param address the address to test
     * @return true if the address is not registered, false instead
     * */
    def isNotRegistered(address: SocketAddress): Boolean = {
        !connections.contains(address)
    }

    private def checkAddress(address: SocketAddress): Unit = {
        if (connections.contains(address))
            throw RelayInitialisationException("this address is already registered !")
    }

}
