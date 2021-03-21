/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.server.connection

import fr.`override`.linkit.api.connection.ConnectionException
import fr.`override`.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.`override`.linkit.api.local.system.{JustifiedCloseable, Reason}
import fr.`override`.linkit.core.local.concurrency.PacketWorkerThread
import fr.`override`.linkit.core.local.system.ContextLogger
import fr.`override`.linkit.server.ServerException
import fr.`override`.linkit.server.network.ServerNetwork
import org.jetbrains.annotations.Nullable

import scala.collection.mutable
import scala.util.control.NonFatal

/**
 * TeamMate of RelayServer, handles the RelayPoint Connections.
 *
 * @see [[RelayServer]]
 * @see [[ServerExternalConnection]]
 * */
class ExternalConnectionsManager(server: ServerConnection, serverNetwork: ServerNetwork) extends JustifiedCloseable {

    /**
     * java map containing all RelayPointConnection instances
     * */
    private val connections: mutable.Map[String, ServerExternalConnection] = mutable.Map.empty
    private val maxConnection = server.configuration.maxConnection

    @volatile private var closed = false


    override def close(reason: Reason): Unit = {
        for ((_, connection) <- connections) try {
            ContextLogger.trace(s"Closing '${connection.supportIdentifier}'...")
            connection.close(reason)
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
        closed = true
    }

    /**
     * creates and register a connection.
     *
     * @param socket the socket to start the connection
     * @param identifier the connection's identifier
     * @throws ConnectionException if the provided identifier is already taken.
     * @throws ServerException if the registered connection count exceeded configuration limit.
     * */
    @throws[ConnectionException]("if the provided identifier is already taken")
    @throws[ServerException]("if the registered connection count exceeded configuration limit.")
    def createConnection(identifier: String,
                         socket: SocketContainer): Unit = {
        ContextLogger.info(s"Registering connection '$identifier' (${socket.remoteSocketAddress()})...")
        if (connections.contains(identifier))
            throw ConnectionException(connections(identifier), s"This connection identifier is taken ! ('$identifier')")

        if (connections.size > maxConnection)
            throw ServerException(server, "Maximum connection limit exceeded")

        //Opening ClientConnection and finalizing registration
        val connectionSession = ConnectionSession(identifier, socket, server, this, serverNetwork)
        val connection = ServerExternalConnection.open(connectionSession)
        connections.put(identifier, connection)

        println("Sending authorisation packet...")
        server.sendAuthorisedConnection(socket)

        val canConnect = server.securityManager.checkConnection(connection)
        if (canConnect) {
            println(s"Connection of '$identifier' was successfully registered !")
            return
        }

        val msg = "Connection rejected by security manager"
        connection.getConsoleErr.println(msg)
        Console.err.println(s"Relay Connection '$identifier': " + msg)

        connections.remove(identifier)
        connection.close(Reason.SECURITY_CHECK)
    }

    /* def broadcastMessage(err: Boolean, msg: String): Unit = {
         connections.values
                 .foreach(connection => {
                     if (err)
                         connection.getConsoleErr.println(msg)
                     else connection.getConsoleOut.println(msg)
                 })
     }*/

    /**
     * Broadcast bytes sequence to every connected clients
     * */
    def broadcastBytes(packet: Packet, injectableID: Int, senderID: String, discardedIDs: String*): Unit = {
        PacketWorkerThread.checkNotCurrent()
        connections.values
                .filter(con => !discardedIDs.contains(con.supportIdentifier) && con.isConnected)
                .foreach(connection => {
                    val translator = connection.translator
                    val coordinates = DedicatedPacketCoordinates(injectableID, connection.supportIdentifier, senderID)
                    val result = translator.translate(packet, coordinates)
                    connection.send(result)
                })
    }

    /**
     * unregisters a Relay point
     *
     * @param identifier the identifier to disconnect
     * */
    def unregister(identifier: String): Option[ServerExternalConnection] = {
        connections.remove(identifier)
    }


    /**
     * retrieves a RelayPointConnection based on the address
     *
     * @param identifier the identifier linked [[ServerExternalConnection]]
     * @return the found [[ServerExternalConnection]] bound with the identifier
     * */
    @Nullable
    def getConnection(identifier: String): ServerExternalConnection = connections.get(identifier).orNull

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
        identifier == server.supportIdentifier || connections.contains(identifier) //reserved server identifier
    }

    override def isClosed: Boolean = closed

    /**
     * Deflects a packet to his associated [[ServerExternalConnection]]
     *
     * @throws RelayException if no connection where found for this packet.
     * @param bytes the packet bytes to deflect
     * */
    private[connection] def deflectTo(bytes: Array[Byte], target: String): Unit = {
        val connection = getConnection(target)
        if (connection == null)
            throw new ServerException(server, s"unknown ID '$target' to deflect packet")
        connection.send(bytes)
    }
}