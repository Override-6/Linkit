package fr.overridescala.vps.ftp.server

import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.nio.charset.Charset

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.api.packet.{DataPacket, PacketLoader, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.{TaskAction, TaskCompleterHandler, TaskConcoctor}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.server.connection.ConnectionsManager
import fr.overridescala.vps.ftp.server.task.ServerTasksHandler
import org.jetbrains.annotations.Nullable

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters

class RelayServer()
        extends Relay {


    private val selector = Selector.open()
    private val buffer = ByteBuffer.allocateDirect(Constants.MAX_PACKET_LENGTH)

    private val serverSocket = configSocket()
    private val tasksHandler = new ServerTasksHandler(this)
    private val connectionsManager = new ConnectionsManager(tasksHandler)
    private val packetLoader = new PacketLoader()
    /**
     * this channel is only open / must be only used to register a newly socket connection.
     * it can't be used to send or receive ordinary packets
     * */
    @Nullable
    @volatile private var currentSocketInitialisationChannel: SimplePacketChannel = _

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
                        if (id == null)
                            return
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
        val socket = serverSocket.accept()
        val address = socket.getRemoteAddress
        socket.configureBlocking(false)
        socket.register(selector, SelectionKey.OP_READ)
        println(s"new connection : ${address}")
        Future {
            registerConnection(socket)
        }
    }

    private def registerConnection(socket: SocketChannel): Unit = {
        val channel = new SimplePacketChannel(socket, -1)
        channel.sendPacket("GID")
        currentSocketInitialisationChannel = channel
        val identifier = channel.nextPacket().header
        var response = "OK"
        try {
            connectionsManager.register(socket.getRemoteAddress, identifier)
            println(s"successfully registered relay point with identifier '$identifier'")
        } catch {
            case e: RelayInitialisationException =>
                Console.err.println(e.getMessage)
                response = "ERROR"
        }
        channel.sendPacket(response)
        currentSocketInitialisationChannel = null
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

        val ownerID = connectionsManager.getIdentifierFromAddress(address)

        while (packet != null) {
            if (currentSocketInitialisationChannel != null) {
                currentSocketInitialisationChannel.addPacket(packet)
                return
            }
            else tasksHandler.handlePacket(packet, ownerID, socket)

            packet = packetLoader.nextPacket
        }
    }


    private def configSocket(): ServerSocketChannel = {
        val socket = ServerSocketChannel.open()
        socket.configureBlocking(false)
        socket.bind(Constants.LOCALHOST)
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
