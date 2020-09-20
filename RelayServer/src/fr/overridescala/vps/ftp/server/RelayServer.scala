package fr.overridescala.vps.ftp.server

import java.net.{InetSocketAddress, ServerSocket, Socket, SocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.nio.charset.Charset
import java.util

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketLoader, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.{CreateFileTask, DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{TaskAction, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescription}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.server.connection.RelayPointConnectionManager

import scala.collection.mutable
import scala.jdk.CollectionConverters

class RelayServer()
        extends Relay {

    private val selector = Selector.open()
    private val buffer = ByteBuffer.allocate(Constants.MAX_PACKET_LENGTH)

    private val serverSocket = configSocket()
    private val tasksHandler = new TasksHandler()
    private val completerFactory = new ServerTaskCompleterFactory(tasksHandler, this)
    private val connectionsManager = new RelayPointConnectionManager(tasksHandler)
    private val packetLoader = new PacketLoader()

    private var open = true

    override val identifier: String = Constants.SERVER_ID

    override def doDownload(description: TransferDescription): TaskAction[Unit] = {
        val target = description.targetID
        new DownloadTask(getPacketChannel(target), tasksHandler, description)
    }

    override def doUpload(description: TransferDescription): TaskAction[Unit] = {
        val target = description.targetID
        new UploadTask(getPacketChannel(target), tasksHandler, description)
    }

    override def requestFileInformation(ownerID: String, path: String): TaskAction[FileDescription] =
        new FileInfoTask(getPacketChannel(ownerID), tasksHandler, ownerID, path)

    override def requestCreateFile(ownerID: String, path: String): TaskAction[Unit] =
        new CreateFileTask(path, ownerID, getPacketChannel(ownerID), tasksHandler)

    override def start(): Unit = {
        println("ready !")
        println("current encoding is " + Charset.defaultCharset().name())
        println(s"listening on port ${Constants.PORT}")
        tasksHandler.start()

        while (open) {
            val clientSocket = serverSocket.accept()
            val bytes = clientSocket.getInputStream.readAllBytes()
            println(s"bytes = ${bytes.mkString("Array(", ", ", ")")}")
            handleNewConnection(clientSocket)
        }
    }

    override def close(): Unit = {
        open = false
        serverSocket.close()
        selector.selectedKeys().forEach(key => {
            key.cancel()
            key.channel().close()
        })
        selector.close()
    }

    /**
     * disconnects a RelayPoint connection
     *
     * @param identifier the identifier of the RelayPoint connection
     * */
    def disconnect(identifier: String): Unit = {
        val keys = toScalaSet(selector.selectedKeys())
        for (key <- keys) {
            val address = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
            val keyId = connectionsManager.getConnectionFromAddress(address).id
            if (keyId.equals(identifier)) {
                key.channel().close()
                key.cancel()
                connectionsManager.disconnect(address)
                return
            }
        }
    }

    /**
     * @throws IllegalArgumentException if the identifier is the same as this server Identifier
     * @return the PacketChannel of the connection, represented by his identifier
     * */
    private def getPacketChannel(identifier: String): SimplePacketChannel = {
        if (identifier.equals(this.identifier))
            throw new IllegalArgumentException("requested Packet Channel of server")
        connectionsManager.getConnectionFromIdentifier(identifier).packetChannel
    }

    /**
     * handles a new RelayPoint connection.
     * */
    private def handleNewConnection(socket: Socket): Unit = {
        val address = socket.getRemoteSocketAddress
        connectionsManager.createConnection(socket, bytes => onConnectionUpdate(bytes, address))
        println(s"new connection : ${address}")
    }

    /**
     * read / refreshes a connection
     *
     * @param bytes the bytes read from the socket
     * @param address the socket address
     * */
    private def onConnectionUpdate(bytes: Array[Byte], address: SocketAddress ): Unit = {
        handleBytes(bytes)
        handlePacket(address)
    }

    /**
     * prints the bytes into the buffer and update packetLoader
     *
     * @param bytes the bytes to perform
     * @return the number of bytes read
     * */
    private def handleBytes(bytes: Array[Byte]): Unit = {
        buffer.flip()
        buffer.get(bytes)
        packetLoader.add(bytes)
        buffer.clear()
    }

    /**
     * handles the data, then creates one or multiple packets in function of the PacketLoader.
     * then distribute the packets to other handlers
     * */
    def handlePacket(address: SocketAddress): Unit = {
        var packet: DataPacket = packetLoader.nextPacket
        if (packet == null)
            return

        val connection = connectionsManager.getConnectionFromAddress(address: SocketAddress)
        if (connection == null) {
            handleInit(packet, address)
            return
        }

        val identifier = connection.id
        val packetChannel = getPacketChannel(identifier)
        while (packet != null) {
            tasksHandler.handlePacket(packet, completerFactory, packetChannel)
            packet = packetLoader.nextPacket
        }
    }

    /**
     * handles an presumed INIT Packet, then init the RelayPointConnection
     *
     * @throws UnexpectedPacketException if the packet header isn't "INIT"
     * */
    def handleInit(packet: DataPacket, address: SocketAddress): Unit = {
        val header = packet.header
        if (!header.equals("INIT"))
            throw UnexpectedPacketException(s"received packet $packet when attempting to init a RelayPointConnection")
        val identifier = new String(packet.content)
        connectionsManager.initConnection(address, identifier)
    }

    private def configSocket(): ServerSocket = {
        val socket = new ServerSocket(Constants.PORT)
        socket
    }

    private def toScalaSet[T](javaSet: java.util.Set[T]): mutable.Set[T] =
        CollectionConverters.SetHasAsScala(javaSet).asScala

    // default tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close()))

}
