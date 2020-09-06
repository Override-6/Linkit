package fr.overridescala.vps.ftp.server

import java.io.IOException
import java.net.{InetSocketAddress, SocketAddress, SocketException}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.nio.file.Path
import java.util
import java.util.concurrent.ConcurrentHashMap

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.{PacketChannel, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.{DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{Task, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}
import fr.overridescala.vps.ftp.api.utils.{Constants, Protocol}

import scala.collection.mutable
import scala.jdk.CollectionConverters

class RelayServer(private val id: String)
        extends Relay {
    private val selector = Selector.open()


    private val serverSocket = configSocket()
    private val tasksHandler = new TasksHandler()
    private val completerFactory = new ServerTaskCompleterFactory(tasksHandler, this)
    private val keysPacketChannel = new ConcurrentHashMap[SocketAddress, SimplePacketChannel]()
    override val identifier: String = this.id


    override def doDownload(description: TransferDescription): Task[Unit] = {
        val target = description.target
        new DownloadTask(getPacketChannel(target), tasksHandler, description)
    }

    override def doUpload(description: TransferDescription): Task[Unit] = {
        val target = description.target
        new UploadTask(getPacketChannel(target), tasksHandler, description)
    }

    override def requestAddress(id: String): Task[InetSocketAddress] =
        throw new UnsupportedOperationException("can't create this request from a RelayServer, please use RelayServer#getAddress instead")

    override def requestFileInformation(owner: InetSocketAddress, path: String): Task[TransferableFile] =
        new FileInfoTask(getPacketChannel(owner), tasksHandler, owner, path)

    override def start(): Unit = {
        println("ready !")
        tasksHandler.start()

        while (true) {
            selector.select()
            val it: util.Iterator[SelectionKey] = selector.selectedKeys().iterator()
            while (it.hasNext) {
                handleKey(it.next())
                it.remove()
            }
        }
    }

    override def close(): Unit = {
        serverSocket.close()
        selector.selectedKeys().forEach(_.channel().close())
        selector.close()
    }

    def disconnect(address: InetSocketAddress): Unit = {
        val keys = toScalaSet(selector.selectedKeys())
        for (key <- keys) {
            val socketChannel = key.channel().asInstanceOf[SocketChannel]
            if (address.equals(socketChannel.getRemoteAddress)) {
                key.channel().close()
                key.cancel()
            }
        }
    }

    def getAddress(id: String): InetSocketAddress = {
        if (id.equals(this.id))
            return Constants.PUBLIC_ADDRESS


        val keys = toScalaSet(selector.selectedKeys())
        //TODO send a init packet when a new client connects
        //FIXME it does not work !
        for (key <- keys) {
            val socketChannel: SocketChannel = key.channel().asInstanceOf[SocketChannel]
            return socketChannel.getRemoteAddress.asInstanceOf[InetSocketAddress]
        }
        null
    }


    private def getPacketChannel(target: InetSocketAddress): PacketChannel = {
        val keys: util.Set[SelectionKey] = selector.selectedKeys()
        val it = toScalaSet(keys)
        for (key <- it) {
            val channel: SocketChannel = key.channel().asInstanceOf[SocketChannel]
            if (channel.getRemoteAddress equals target)
                return keysPacketChannel.get(key)
        }
        null
    }

    private def handleNewConnection(): Unit = {
        val channel = serverSocket.accept()
        channel.configureBlocking(false)
        channel.register(selector, SelectionKey.OP_READ)
        val socketChannel = channel.asInstanceOf[SocketChannel]
        keysPacketChannel.put(socketChannel.getRemoteAddress, new SimplePacketChannel(channel))
    }


    private def handleKey(key: SelectionKey): Unit = {
        if (key.isAcceptable)
            handleNewConnection()
        if (key.isReadable)
            handlePacket(key)
    }

    private def handlePacket(key: SelectionKey): Unit = {
        val channel = key.channel().asInstanceOf[SocketChannel]
        val buffer = ByteBuffer.allocate(Constants.MAX_PACKET_LENGTH)
        val count = channel.read(buffer)
        val bytes = new Array[Byte](count)
        buffer.flip()
        buffer.get(bytes)
        val packetChannel = keysPacketChannel.get(channel.getRemoteAddress)
        val packet = Protocol.toPacket(bytes)

        if (tasksHandler.handlePacket(packet, completerFactory, packetChannel))
            return
        packetChannel.addPacket(packet)
    }

    private def configSocket(): ServerSocketChannel = {
        val socket = ServerSocketChannel.open()
        socket.configureBlocking(false)
        socket.bind(Constants.PUBLIC_ADDRESS)
        socket.register(selector, SelectionKey.OP_ACCEPT)
        socket
    }

    private def toScalaSet[T](javaSet: java.util.Set[T]): mutable.Set[T] = {
        CollectionConverters.SetHasAsScala(javaSet).asScala
    }

}
