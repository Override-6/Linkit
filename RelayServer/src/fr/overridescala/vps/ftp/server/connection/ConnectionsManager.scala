package fr.overridescala.vps.ftp.server.connection

import java.net.SocketAddress
import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.server.ServerTasksHandler

import scala.collection.mutable

/**
 * TeamMate of RelayServer, handles the RelayPoint Connections.
 *
 * @see RelayServer
 * @see RelayPointConnection
 * */
class ConnectionsManager(private val tasksHandler: ServerTasksHandler) {
    /**
     * java map containing all RelayPointConnection instances
     * */
    private val connections: mutable.Map[SocketAddress, String] = mutable.Map.empty[SocketAddress, String]


    /**
     * creates and register a RelayPoint connection.
     *
     * @param socket     the socket connection
     * @param identifier the identifier for the connection
     * @throws IllegalArgumentException when a id is already set for this address, or another connection is known under this id.
     * */
    def register(socket: SocketChannel, identifier: String): Unit = {
        val address = socket.getRemoteAddress

        if (connections.contains(address))
            throw new IllegalAccessException("this socket is already registered !")

        for ((_, id) <- connections if id != null) {
            if (id.equals(identifier)) {
                throw new IllegalArgumentException(s"another relay point have the same identifier")
            }
        }
        connections.put(address, identifier)
    }

    /**
     * disconnects a RelayPointConnection
     *
     * @param address the address to disconnect
     * */
    def disconnect(address: SocketAddress): Unit = {
        tasksHandler.cancelTasks(getIdentifierFromAddress(address))
        connections.remove(address)
    }

    def getIdentifierFromAddress(address: SocketAddress): String = {
        connections(address)
    }

    /**
     * get a RelayPointConnection based on the address
     *
     * @param identifier the identifier to retrieve the linked connection
     * @return the associated RelayPoinConnection instance, null instead
     * */
    def getAddressFromIdentifier(identifier: String): SocketAddress = {
        for ((address, id) <- connections) {
            if (id.equals(identifier))
                return address
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

}
