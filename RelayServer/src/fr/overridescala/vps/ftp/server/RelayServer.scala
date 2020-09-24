package fr.overridescala.vps.ftp.server

import java.net.{InetSocketAddress, SocketException}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.nio.charset.Charset

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException
import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketLoader, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.{CreateFileTask, DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{TaskAction, TaskCompleterHandler, TaskConcoctor}
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescription}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.server.connection.ConnectionsManager

import scala.collection.mutable
import scala.jdk.CollectionConverters

class RelayServer()
        extends Relay {

    private val selector = Selector.open()
    private val buffer = ByteBuffer.allocateDirect(Constants.MAX_PACKET_LENGTH)

    private val serverSocket = configSocket()
    private val tasksHandler = new ServerTasksHandler(this)
    private val connectionsManager = new ConnectionsManager(tasksHandler)
    private val packetLoader = new PacketLoader()

    private var open = true

    override val identifier: String = Constants.SERVER_ID

    override def scheduleTask[R, T >: TaskAction[R]](concoctor: TaskConcoctor[R]): TaskAction[R] = {
        ensureOpen()
        concoctor.concoct(tasksHandler)
    }

    override def getCompleterFactory: TaskCompleterHandler = tasksHandler.getTasksCompleterHandler

    override def start(): Unit = {
        println("ready !")
        println("current encoding is " + Charset.defaultCharset().name())
        println("listening on port " + Constants.PORT)

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
                        if (e.getMessage == null || !e.getMessage.equalsIgnoreCase("Connection reset"))
                            e.printStackTrace()
                        val address = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
                        val id = connectionsManager.getIdentifierFromAddress(address)
                        println("a connection closed suddenly")
                        disconnect(id)
                }
                it.remove()
            }
        }
    }

    /**
     * disconnects a RelayPoint connection
     *
     * @param identifier the identifier of the RelayPoint connection
     * */
    private def disconnect(identifier: String): Unit = {
        val keys = toScalaSet(selector.selectedKeys())
        for (key <- keys) {
            val address = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
            val keyId = connectionsManager.getIdentifierFromAddress(address)
            if (keyId.equals(identifier)) {
                key.channel().close()
                key.cancel()
                connectionsManager.disconnect(address)
                tasksHandler.cancelTasks(keyId)
                return
            }
        }
    }

    override def close(): Unit = {
        open = false
        serverSocket.close()
        selector.selectedKeys().forEach(key => {
            val address = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
            disconnect(connectionsManager.getIdentifierFromAddress(address))
        })
        selector.close()
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

    private def handleKey(key: SelectionKey): Unit = {
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

        val ownerID = connectionsManager.getIdentifierFromAddress(address)
        while (packet != null) {
            tasksHandler.handlePacket(packet, ownerID, socket)
            packet = packetLoader.nextPacket
        }
    }

    /**
     * handles an presumed INIT Packet, then init the RelayPointConnection
     *
     * @throws UnexpectedPacketException if the packet header isn't "INIT"
     * */
    private def handleInit(packet: DataPacket, socket: SocketChannel): Unit = {
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

    private def ensureOpen(): Unit = {
        if (!open)
            throw new UnsupportedOperationException("Relay Point have to be started !")
    }

    // default tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close()))

}
