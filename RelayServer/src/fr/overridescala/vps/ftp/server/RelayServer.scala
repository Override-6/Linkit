package fr.overridescala.vps.ftp.server

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.nio.charset.Charset

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketLoader, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.{CreateFileTask, DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{TaskAction, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescription}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.server.connection.ChannelsManager

import scala.collection.mutable
import scala.jdk.CollectionConverters

class RelayServer()
        extends Relay {

    private val selector = Selector.open()
    private val buffer = ByteBuffer.allocateDirect(Constants.MAX_PACKET_LENGTH)

    private val serverSocket = configSocket()
    private val tasksHandler = new TasksHandler()
    private val completerFactory = new ServerTaskCompleterFactory(tasksHandler, this)
    private val connectionsManager = new ChannelsManager(tasksHandler)
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
        println("listening on port " + Constants.PORT)
        tasksHandler.start()

        while (open) {
            selector.select()
            if (!open) {
                println("CLOSED")
                return
            }
            val it = selector.selectedKeys().iterator()
            while (it.hasNext) {
                val key = it.next()
                try {
                    updateKey(key)
                } catch {
                    case e: Throwable =>
                        e.printStackTrace()
                        val address = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
                        val id = connectionsManager.getChannelFromAddress(address).ownerID
                        println("a connection closed suddenly")
                        disconnect(id)
                }
                it.remove()
            }
        }
    }

    override def close(): Unit = {
        open = false
        serverSocket.close()
        selector.selectedKeys().forEach(key => {
            val address = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
            disconnect(connectionsManager.getChannelFromAddress(address).ownerID)
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
            val keyId = connectionsManager.getChannelFromAddress(address).ownerID
            if (keyId.equals(identifier)) {
                key.channel().close()
                key.cancel()
                connectionsManager.disconnect(address)
                tasksHandler.cancelTasks(keyId)
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
        connectionsManager.getChannelFromIdentifier(identifier)
    }

    /**
     * handles a new RelayPoint connection.
     * */
    private def handleNewConnection(): Unit = {
        val channel = serverSocket.accept()
        channel.configureBlocking(false)
        channel.register(selector, SelectionKey.OP_READ)
        println(s"new connection : ${channel.getRemoteAddress}")
    }


    /**
     * updates key
     *
     * @param key the key to update
     * */
    private def updateKey(key: SelectionKey): Unit = {
        if (key.isAcceptable)
            handleNewConnection()
        if (key.isReadable)
            handleKey(key)
    }

    def handleKey(key: SelectionKey): Unit = {
        readKey(key)
        handlePacket(key)
    }

    /**
     * prints the bytes into the buffer and update packetLoader
     *
     * @return the number of bytes read
     * */
    private def readKey(key: SelectionKey): Unit = {
        val channel = key.channel().asInstanceOf[SocketChannel]

        val count = channel.read(buffer)
        if (count < 1)
            return

        val bytes = new Array[Byte](count)

        buffer.flip()
        buffer.get(bytes)
        packetLoader.add(bytes)
        buffer.clear()
    }

    /**
     * handles the data, then creates one or multiple packets in function of the PacketLoader.
     * then distribute the packets to other handlers
     * */
    private def handlePacket(key: SelectionKey): Unit = {
        var packet: DataPacket = packetLoader.nextPacket
        if (packet == null)
            return

        val socket = key.channel().asInstanceOf[SocketChannel]
        val address = socket.getRemoteAddress

        if (connectionsManager.isNotRegistered(address)) {
            handleInit(packet, socket)
            return
        }

        val packetChannel = connectionsManager.getChannelFromAddress(address)
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
    def handleInit(packet: DataPacket, socket: SocketChannel): Unit = {
        val header = packet.header
        if (!header.equals("INIT"))
            throw UnexpectedPacketException(s"received packet $packet when attempting to init a RelayPointConnection")
        val identifier = new String(packet.content)
        connectionsManager.register(socket, identifier)
        println(s"registered connection with identifier ${identifier}")
    }

    private def configSocket(): ServerSocketChannel = {
        val socket = ServerSocketChannel.open()
        socket.configureBlocking(false)
        socket.bind(Constants.PUBLIC_ADDRESS)
        socket.register(selector, SelectionKey.OP_ACCEPT)
        socket
    }

    private def toScalaSet[T](javaSet: java.util.Set[T]): mutable.Set[T] =
        CollectionConverters.SetHasAsScala(javaSet).asScala

    // default tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close()))

}
