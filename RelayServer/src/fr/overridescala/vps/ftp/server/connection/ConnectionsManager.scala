package fr.overridescala.vps.ftp.server.connection

import java.net.{Socket, SocketAddress}
import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.server.RelayServer
import org.jetbrains.annotations.Nullable

import scala.collection.mutable

/**
 * TeamMate of RelayServer, handles the RelayPoint Connections.
 *
 * @see [[RelayServer]]
 * @see [[ClientConnectionThread]]
 * */
class ConnectionsManager(server: RelayServer) {
    /**
     * java map containing all RelayPointConnection instances
     * */
    private val connections: mutable.Map[SocketAddress, ClientConnectionThread] = mutable.Map.empty

    /**
     * creates and register a RelayPoint connection.
     *
     * @param identifier the identifier for the connection
     * @throws RelayInitialisationException when a id is already set for this address, or another connection is known under this id.
     * */
    def register(socket: Socket, identifier: String): Unit = {
        val address = socket.getRemoteSocketAddress
        checkAddress(address)
        for ((_, connection) <- connections) {
            if (connection.identifier.equals(identifier))
                throw RelayInitialisationException(s"another relay point have the same identifier '$identifier'")
        }
        val connection = new ClientConnectionThread(socket, identifier, server)
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

    @Nullable def getIdentifierFromAddress(address: SocketAddress): String = {
        if (!connections.contains(address))
            return null
        connections(address).identifier
    }

    /**
     * get a RelayPointConnection based on the address
     *
     * @param identifier the identifier to retrieve the linked connection
     * @return the associated RelayPoinConnection instance, null instead
     * */
    def getAddressFromIdentifier(identifier: String): SocketAddress = {
        for ((address, connection) <- connections) {
            if (connection.identifier.equals(identifier))
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

    private def checkAddress(address: SocketAddress): Unit = {
        if (connections.contains(address))
            throw RelayInitialisationException("this address is already registered !")
    }

}
