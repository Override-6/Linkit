package fr.overridescala.vps.ftp.server

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util
import java.util.concurrent.ConcurrentHashMap

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.SimplePacketChannel
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
    private val keysInfo = new ConcurrentHashMap[SocketAddress, KeyInfo]()
    private var open = true

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
        throw new UnsupportedOperationException("can't create this request from a RelayServer, please use RelayServer#getAddress instead.")

    override def requestFileInformation(owner: InetSocketAddress, path: String): Task[TransferableFile] =
        new FileInfoTask(getPacketChannel(owner), tasksHandler, owner, path)

    override def start(): Unit = {
        println("ready !")
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
                    case _: Throwable =>
                        val address = key.channel().asInstanceOf[SocketChannel].getRemoteAddress
                        tasksHandler.cancelTasks(address)
                        key.cancel()
                        println("a connection closed suddenly")
                        disconnect(address)
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
        println(s"keys = ${keys}")
        for (key <- keys) {
            val socketChannel = key.channel().asInstanceOf[SocketChannel]

            println(s"address = ${address}")
            println(s"socketChannel.getRemoteAddress = ${socketChannel.getRemoteAddress}")

            if (address.equals(socketChannel.getRemoteAddress)) {
                println("TROUVAI UNE POURRE " + address )
                key.channel().close()
                key.cancel()
                keysInfo.remove(address)
                tasksHandler.cancelTasks(address)
            }
        }
    }

    def getAddress(id: String): InetSocketAddress = {
        if (id.equals(this.id))
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

        val packetChannel = getPacketChannel(channel.getRemoteAddress)
        val packet = Protocol.toPacket(bytes)
        tasksHandler.handlePacket(packet, completerFactory, packetChannel)
    }

    private def configSocket(): ServerSocketChannel = {
        val socket = ServerSocketChannel.open()
        socket.configureBlocking(false)
        socket.bind(Constants.LOCALHOST)
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
