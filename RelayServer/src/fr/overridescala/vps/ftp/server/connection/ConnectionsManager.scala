package fr.overridescala.vps.ftp.server.connection

import java.net.SocketAddress
import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.server.task.ServerTasksHandler
import org.jetbrains.annotations.Nullable

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
     * @param address     the address to bind
     * @param identifier the identifier for the connection
     * @throws RelayInitialisationException when a id is already set for this address, or another connection is known under this id.
     * */
    def register(address: SocketAddress, identifier: String): Unit = {
        checkAddress(address)
        for ((_, id) <- connections if id != null) {
            if (id.equals(identifier))
                throw RelayInitialisationException(s"another relay point have the same identifier '$identifier'")
        }
        connections.put(address, identifier)
    }

    /**
     * disconnects a Relay point
     *
     * @param address the address to disconnect
     * */
    def disconnect(address: SocketAddress): Unit = {
        tasksHandler.cancelTasks(getIdentifierFromAddress(address))
        connections.remove(address)
    }

    @Nullable def getIdentifierFromAddress(address: SocketAddress): String = {
        if (!connections.contains(address))
            return null
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

    private def checkAddress(address: SocketAddress): Unit = {
        if (connections.contains(address))
            throw RelayInitialisationException("this address is already registered !")
    }

}
