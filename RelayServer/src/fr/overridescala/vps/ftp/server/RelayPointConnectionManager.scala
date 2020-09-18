package fr.overridescala.vps.ftp.server

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.channels.SocketChannel
import java.util

import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel
import fr.overridescala.vps.ftp.api.task.TasksHandler

import scala.jdk.CollectionConverters

/**
 * TeamMate of RelayServer, handles the RelayPoint Connections.
 * @see RelayServer
 * @see RelayPointConnection
 * */
class RelayPointConnectionManager(private val tasksHandler: TasksHandler) {

    /**
     * java map containing all RelayPointConnection instances
     * */
    private val connections: util.Map[SocketAddress, RelayPointConnection] = new util.HashMap[SocketAddress, RelayPointConnection]()


    /**
     * attributes a identifier to a connected Relay Point.
     *
     * @param address    the address in which we attribute the identifier
     * @param identifier the identifier of the address/connection
     * @throws IllegalArgumentException when a id is already set for this address, or another connection is known under this id.
     * */
    def initConnection(socket: SocketChannel, identifier: String): Unit = {
        val scalaMap = CollectionConverters.MapHasAsScala(connections).asScala
        val address = socket.getRemoteAddress
        if (connections.containsKey(address))
            throw new IllegalArgumentException(s"RelayPointConnection is already set for address $address")

        for ((_, info) <- scalaMap if info.identifier != null) {
            if (info.identifier.equals(identifier)) {
                throw new IllegalArgumentException(s"another relay point have the same identifier")
            }
        }

        val packetChannel = new SimplePacketChannel(socket, "", tasksHandler)

        val info = RelayPointConnection(identifier, packetChannel, address)
        connections.put(address, info)
        println(s"connections = ${connections}")
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
        println(s"connections = ${connections}")
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

}
