package fr.overridescala.vps.ftp.server

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.nio.charset.Charset
import java.util
import java.util.concurrent.ConcurrentHashMap

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketLoader, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.{CreateFileTask, DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{Task, TaskAction, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, FileDescription}
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
                        val identifier = keysInfo.get(address).identifier
                        tasksHandler.cancelTasks(identifier)
                        key.cancel()
                        println("a connection closed suddenly")
                        disconnect(identifier)
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

    def disconnect(id: String): Unit = {
        val keys = toScalaSet(selector.selectedKeys())
        for (key <- keys) {
            val address = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
            val keyId = keysInfo.get(address).identifier
            if (keyId.equals(id)) {
                key.channel().close()
                key.cancel()
                keysInfo.remove(address)
                tasksHandler.cancelTasks(identifier)
                return
            }
        }
    }

    def getAddress(id: String): InetSocketAddress = {
        if (id.equals(identifier))
            return Constants.PUBLIC_ADDRESS

        val scalaKeysInfo = CollectionConverters.MapHasAsScala(keysInfo).asScala
        for ((_, info) <- scalaKeysInfo if info.identifier != null) {
            if (info.identifier.equals(id))
                return info.address.asInstanceOf[InetSocketAddress]
        }
        null
    }

    def attributeID(address: InetSocketAddress, id: String): Boolean = {
        val scalaKeysInfo = CollectionConverters.MapHasAsScala(keysInfo).asScala
        val info = keysInfo.get(address)
        val relayPointID = info.identifier
        if (relayPointID != null) {
            return false
        }
        for ((_, info) <- scalaKeysInfo if info.identifier != null) {
            if (info.identifier.equals(id)) {
                return false
            }
        }
        info.identifier = id
        true
    }

    private def getPacketChannel(target: String): SimplePacketChannel = {
        keysInfo.get(target).channelManager
    }

    private def handleNewConnection(): Unit = {
        val channel = serverSocket.accept()
        channel.configureBlocking(false)
        channel.register(selector, SelectionKey.OP_READ)
        val socketChannel = channel.asInstanceOf[SocketChannel]
        val info = KeyInfo(socketChannel.getRemoteAddress, null, new SimplePacketChannel(channel, "", tasksHandler))
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

        val identifier = keysInfo.get(channel.getRemoteAddress).identifier
        val packetChannel = getPacketChannel(identifier)
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
                       var identifier: String,
                       channelManager: SimplePacketChannel)

}
