package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.exception.{RelayException, RelayInitialisationException}
import fr.`override`.linkit.api.packet.{SimpleTrafficHandler, TrafficHandler}
import fr.`override`.linkit.api.system.{CloseReason, JustifiedCloseable}
import fr.`override`.linkit.server.RelayServer
import fr.`override`.linkit.server.connection.ConnectionsManager.ConnectionContainer
import fr.`override`.linkit.server.exceptions.ConnectionException

import scala.collection.mutable

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
    private val connections: mutable.Map[String, ConnectionContainer] = mutable.Map.empty


    override def close(reason: CloseReason): Unit = {
        for ((_, container) <- connections if container.isInitialised) {
            val connection = container.getConnection
            println(s"Closing '${connection.identifier}'...")
            connection.close(reason)
        }
    }

    /**
     * creates and register a RelayPoint connection.
     *
     * @param socket the socket to start the connection
     * @throws RelayInitialisationException when a id is already set for this address, or another connection is known under this id.
     * */
    def registerConnection(identifier: String,
                           socket: SocketContainer): Unit = {

        if (connections.contains(identifier))
            throw RelayInitialisationException(s"This relay id is already registered ! ('$identifier')")

        if (connections.size > server.configuration.maxConnection)
            throw new RelayException("Maximum connection limit exceeded")

        //Pre initialisation / pre registration
        val trafficHandler = new SimpleTrafficHandler(server, socket)
        val connectionContainer = ConnectionContainer(trafficHandler)
        connections.put(identifier, connectionContainer)

        //Opening ClientConnection and finalizing registration
        val connectionSession = ClientConnectionSession(identifier, socket, server, trafficHandler)
        val connection = ClientConnection.open(connectionSession)
        connectionContainer.connection = Option(connection)
        connectionSession.initNetwork()

        val canConnect = server.securityManager.canConnect(connection)
        if (canConnect) {
            return
        }

        val msg = "Connection rejected by security manager"
        connection.getConsoleErr.println(msg)
        Console.err.println(s"Relay Connection '$identifier': " + msg)

        connections.remove(identifier)
        connection.close(CloseReason.SECURITY_CHECK)
    }

    def broadcast(err: Boolean, msg: String): Unit = {
        connections.values
            .filter(_.isInitialised)
            .map(_.connection.get)
            .foreach(connection => {
                if (err)
                    connection.getConsoleErr.println(msg)
                else connection.getConsoleOut.println(msg)
            })
    }

    /**
     * unregisters a Relay point
     *
     * @param identifier the identifier to disconnect
     * */
    def unregister(identifier: String): ClientConnection = {
        val container = connections.remove(identifier)
        if (container.isDefined && container.get.isInitialised)
            return container.get.getConnection
        null
    }


    /**
     * retrieves a RelayPointConnection based on the address
     *
     * @param identifier the identifier linked [[ClientConnection]]
     * @return the found [[ClientConnection]] bound with the identifier
     * */
    def getConnection(identifier: String): ClientConnection = {
        val container = connections.get(identifier)
        if (container isDefined)
            return container.get.getConnection
        null
    }


    def getConnectionContainer(identifier: String): ConnectionContainer = {
        connections.get(identifier).orNull
    }

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
        connection.sendDeflectedBytes(bytes)
    }


}

object ConnectionsManager {

    case class ConnectionContainer(val trafficHandler: TrafficHandler) {

        private[ConnectionsManager] var connection: Option[ClientConnection] = None

        @throws[ConnectionException]
        def getConnection: ClientConnection = {
            if (connection isEmpty) {
                throw ConnectionException("Unable to return ClientConnection : This connection is currently in initialisation phase")
            }
            connection.get
        }

        def isInitialised: Boolean = connection isDefined
    }

}
