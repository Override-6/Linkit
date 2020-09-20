package fr.overridescala.vps.ftp.server.connection

import java.net.{ServerSocket, Socket, SocketAddress}
import java.nio.channels.{SelectionKey, SocketChannel}
import java.util

import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel
import fr.overridescala.vps.ftp.api.task.TasksHandler

import scala.jdk.CollectionConverters

/**
 * TeamMate of RelayServer, handles the RelayPoint Connections.
 *
 * @see RelayServer
 * @see RelayPointConnection
 * */
class RelayPointConnectionManager(private val tasksHandler: TasksHandler) {
    /**
     * java map containing all RelayPointConnection instances
     * */
    private val connections: util.Map[SocketAddress, RelayPointConnection] = new util.HashMap[SocketAddress, RelayPointConnection]()


    /**
     * creates and register a RelayPoint connection.
     *
     * @param socket     the socket connection
     * @param identifier the identifier for the connection
     * @throws IllegalArgumentException when a id is already set for this address, or another connection is known under this id.
     * */
    def register(socket: SocketChannel, identifier: String): Unit = {
        val scalaMap = CollectionConverters.MapHasAsScala(connections).asScala
        val address = socket.getRemoteAddress

        if (connections.containsKey(address))
            throw new IllegalAccessException("this socket is already registered !")

        for ((_, info) <- scalaMap if info.identifier != null) {
            if (info.identifier.equals(identifier)) {
                throw new IllegalArgumentException(s"another relay point have the same identifier")
            }
        }
        val packetChannel = new SimplePacketChannel(socket, identifier, address, tasksHandler)
        val connection = RelayPointConnection(packetChannel)
        connections.put(address, connection)
    }

    /**
     * disconnects a RelayPointConnection
     *
     * @param address the address to disconnect
     * */
    def disconnect(address: SocketAddress): Unit = {
        val connection = getConnectionFromAddress(address)
        tasksHandler.cancelTasks(connection.identifier)
        connections.remove(address)
    }

    /**
     * get a RelayPointConnection based on the address
     *
     * @param address the address to retrieve the affected connection
     * @return the linked RelayPointConnection instance, null instead
     * */
    def getConnectionFromAddress(address: SocketAddress): RelayPointConnection = {
        connections.get(address)
    }

    /**
     * get a RelayPointConnection based on the address
     *
     * @param identifier the identifier to retrieve the linked connection
     * @return the associated RelayPoinConnection instance, null instead
     * */
    def getConnectionFromIdentifier(identifier: String): RelayPointConnection = {
        val scalaMap = CollectionConverters.MapHasAsScala(connections).asScala
        for ((_, connection) <- scalaMap) {
            if (connection.identifier.equals(identifier))
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
        !connections.containsKey(address)
    }

}
