package fr.overridescala.vps.ftp.server

import java.net.{InetSocketAddress, SocketAddress, SocketException}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.nio.charset.Charset
import java.util
import java.util.concurrent.ConcurrentHashMap

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketLoader, Protocol, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.{DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{Task, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}
import fr.overridescala.vps.ftp.api.utils.Constants

import scala.collection.mutable
import scala.jdk.CollectionConverters

class RelayServer(override val identifier: String)
        extends Relay {

    private val selector = Selector.open()


    private val serverSocket = configSocket()
    private val tasksHandler = new TasksHandler()
    private val completerFactory = new ServerTaskCompleterFactory(tasksHandler, this)
    private val keysInfo = new ConcurrentHashMap[SocketAddress, KeyInfo]()
    private val packetLoader = new PacketLoader()

    private var open = true


    override def doDownload(description: TransferDescription): Task[Unit] = {
        val target = description.target
        new DownloadTask(getPacketChannel(target), tasksHandler, description)
    }

    override def doUpload(description: TransferDescription): Task[Unit] = {
        val target = description.target
        new UploadTask(getPacketChannel(target), tasksHandler, description)
    }

    override def requestAddress(id: String): Task[InetSocketAddress] =
        throw new UnsupportedOperationException("can't create this request from a RelayServer, please use RelayServer#getAddress instead.")

    override def requestFileInformation(owner: InetSocketAddress, path: String): Task[TransferableFile] =
        new FileInfoTask(getPacketChannel(owner), tasksHandler, owner, path)

    override def start(): Unit = {
        println("ready !")
        println("current encoding is " + Charset.defaultCharset().name())
        tasksHandler.start()

        while (open) {
            selector.select()
            if (!open)
                return
            val it: util.Iterator[SelectionKey] = selector.selectedKeys().iterator()
            while (it.hasNext) {
                val key = it.next()
                try {
                    handleKey(key)
                } catch {
                    case e: Throwable =>
                        val address = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
                        tasksHandler.cancelTasks(address)
                        key.cancel()
                        println("a connection closed suddenly")
                        disconnect(address)
                        e.printStackTrace()
                }
                it.remove()
            }
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

    def disconnect(address: SocketAddress): Unit = {
        val keys = toScalaSet(selector.selectedKeys())
        for (key <- keys) {
            val socketChannel = key.channel().asInstanceOf[SocketChannel]

            if (address.equals(socketChannel.getRemoteAddress)) {
                key.channel().close()
                key.cancel()
                keysInfo.remove(address)
                tasksHandler.cancelTasks(address)
            }
        }
    }

    def getAddress(id: String): InetSocketAddress = {
        if (id.equals(identifier))
            return Constants.PUBLIC_ADDRESS

        val scalaKeysInfo = CollectionConverters.MapHasAsScala(keysInfo).asScala
        for ((_, info) <- scalaKeysInfo if info.id != null) {
            if (info.id.equals(id))
                return info.address.asInstanceOf[InetSocketAddress]
        }
        null
    }

    def attributeID(address: InetSocketAddress, id: String): Boolean = {
        val scalaKeysInfo = CollectionConverters.MapHasAsScala(keysInfo).asScala
        val info = keysInfo.get(address)
        val relayPointID = info.id
        if (relayPointID != null) {
            return false
        }
        for ((_, info) <- scalaKeysInfo if info.id != null) {
            if (info.id.equals(id)) {
                return false
            }
        }
        info.id = id
        true
    }

    private def getPacketChannel(target: SocketAddress): SimplePacketChannel = {
        keysInfo.get(target).channelManager
    }

    private def handleNewConnection(): Unit = {
        val channel = serverSocket.accept()
        channel.configureBlocking(false)
        channel.register(selector, SelectionKey.OP_READ)
        val socketChannel = channel.asInstanceOf[SocketChannel]
        val info = KeyInfo(socketChannel.getRemoteAddress, null, new SimplePacketChannel(channel, tasksHandler))
        keysInfo.put(socketChannel.getRemoteAddress, info)
        println(s"new connection : ${socketChannel.getRemoteAddress}")
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
        if  (count < 1)
            return
        
        val bytes = new Array[Byte](count)

        buffer.flip()
        buffer.get(bytes)
        packetLoader.add(bytes)

        val packetChannel = getPacketChannel(channel.getRemoteAddress)
        var packet: DataPacket = packetLoader.nextPacket
        while (packet != null) {
            tasksHandler.handlePacket(packet, completerFactory, packetChannel)
            packet = packetLoader.nextPacket
        }
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

    // default tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => this.close()))

    case class KeyInfo(address: SocketAddress,
                       var id: String,
                       channelManager: SimplePacketChannel)

}
