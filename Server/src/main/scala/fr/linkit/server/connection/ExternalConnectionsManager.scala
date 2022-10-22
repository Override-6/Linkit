/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.server.connection

import fr.linkit.api.application.connection.{ConnectionException, NoSuchConnectionException}
import fr.linkit.api.gnom.packet.DedicatedPacketCoordinates
import fr.linkit.api.gnom.persistence.{PacketDownload, PacketTransfer, PacketUpload}
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.api.internal.system.{JustifiedCloseable, Reason}
import fr.linkit.engine.internal.concurrency.PacketReaderThread
import fr.linkit.server.ServerException
import fr.linkit.server.connection.traffic.DeflectedPacket
import org.jetbrains.annotations.Nullable

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

/**
 * @see [[ServerConnection]]
 * @see [[ServerExternalConnection]]
 * */
class ExternalConnectionsManager(server: ServerConnection) extends JustifiedCloseable {
    
    private val connections   = mutable.HashMap.empty[String, ServerExternalConnection]
    private val sessions      = ListBuffer.empty[ExternalConnectionSession]
    private val maxConnection = server.configuration.maxConnection
    
    @volatile private var closed = false
    
    override def close(reason: Reason): Unit = {
        for ((_, connection) <- connections) try {
            AppLoggers.App.info(s"Shutting down connection '${connection.boundIdentifier}'...")
            connection.shutdown()
        } catch {
            case NonFatal(e) => e.printStackTrace()
        }
        closed = true
    }
    
    /**
     * creates and register a connection.
     *
     * @param socket     the socket to start the connection
     * @param identifier the connection's identifier
     * @throws ConnectionException if the provided identifier is already taken.
     * @throws ServerException     if the registered connection count exceeded configuration limit.
     * */
    @throws[ConnectionException]("if the provided identifier is already taken")
    @throws[ServerException]("if the registered connection count exceeded configuration limit.")
    def registerConnection(identifier: String,
                           socket: SocketContainer): Unit = {
        AppLoggers.App.debug(s"Registering connection '$identifier' (${socket.remoteSocketAddress()})...")
        //Ensure that the connection's identifier that is about to be created isn't registered yet.
        if (connections.contains(identifier))
            throw ConnectionException(connections(identifier), s"This connection identifier is taken ! ('$identifier')")
        
        //Ensure that fixed connection limit is not reached.
        if (connections.size > maxConnection)
            throw ServerException(server, "Maximum connection limit exceeded")
        
        //Opening ClientConnectionSession and finalizing registration...
        val packetReader = new ServerAsyncPacketReader(socket, server, this, identifier)
        val readerThread = new PacketReaderThread(packetReader, identifier)
        
        val info              = ExternalConnectionSessionInfo(server, this, server.getSideNetwork, readerThread)
        val connectionSession = ExternalConnectionSession(identifier, socket, info)
        val connection        = ServerExternalConnection.open(connectionSession)
        connections.put(identifier, connection)
        sessions += connectionSession
        
        server.sendAuthorisedConnection(socket)
    }
    
    /**
     * Broadcast bytes sequence to every connected clients
     * */
    def broadcastPacket(result: PacketUpload, discardedIDs: String*): Unit = {
        PacketReaderThread.checkNotCurrent()
        val candidates = connections.values
                .filter(con => !discardedIDs.contains(con.boundIdentifier) && con.isConnected)
        candidates.foreach(connection => {
            if (connection.canHandlePacketInjection(result))
                connection.send(result)
            //else throw new NoSuchElementException(s"Unable to send packet to ${connection.boundIdentifier}: Could not find any traffic presence at ${new TrafficObjectReference(result.coords.path)} in the receiver engine that could inject this packet.")
        })
    }
    
    /**
     * unregisters a [[ServerExternalConnection]]
     *
     * @param identifier the identifier to disconnect
     * */
    def unregister(identifier: String): Option[ServerExternalConnection] = {
        connections.remove(identifier)
    }
    
    /**
     * retrieves a [[ServerExternalConnection]] based on the address
     *
     * @param identifier the identifier linked [[ServerExternalConnection]]
     * @return the found [[ServerExternalConnection]] bound with the identifier
     * */
    @Nullable
    def getConnection(identifier: String): ServerExternalConnection = connections.get(identifier).orNull
    
    def countConnections: Int = connections.size
    
    def listIdentifiers: Seq[String] = connections.keys.toSeq
    
    def listConnectionSessions: List[ExternalConnectionSession] = sessions.toList
    
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
     * @return true if any connected Engine have the specified identifier
     * */
    def isRegistered(identifier: String): Boolean = {
        identifier == server.currentIdentifier || connections.contains(identifier) //reserved server identifier
    }
    
    override def isClosed: Boolean = closed
    
    /**
     * Deflects a downloaded packet to its targeting [[ServerExternalConnection]]
     *
     * */
    private[connection] def deflect(result: PacketDownload): Unit = {
        val coords = result.coords
        val target = coords match {
            case e: DedicatedPacketCoordinates => e.targetID
            case _                             =>
                throw new IllegalArgumentException("Deflected packet must be provided with DedicatedPacketCoordinates")
        }
        
        val connection = getConnection(target)
        if (connection == null)
            throw NoSuchConnectionException(s"unknown ID '$target' to deflect packet")
        if (connection.canHandlePacketInjection(result)) {
            connection.send(new DeflectedPacket(result.buff, coords))
        }
    }
}