package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Path

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.{PacketChannel, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks.{AddressTask, DownloadTask, FileInfoTask, UploadTask}
import fr.overridescala.vps.ftp.api.task.{Task, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}
import fr.overridescala.vps.ftp.api.utils.{Constants, Protocol}

class RelayPoint(private val id: String,
                 private val serverAddress: InetSocketAddress) extends Relay {


    private val socketChannel = configSocket()
    private val tasksHandler = new TasksHandler()
    private val packetChannel = new SimplePacketChannel(socketChannel)
    private val completerFactory = new RelayPointTaskCompleterFactory(tasksHandler)

    override val identifier: String = id

    override def requestDownload(description: TransferDescription): Task[Unit] =
        new DownloadTask(packetChannel, tasksHandler, description)

    override def requestUpload(description: TransferDescription): Task[Unit] =
        new UploadTask(packetChannel, tasksHandler, description)

    override def requestAddress(id: String): Task[InetSocketAddress] =
        new AddressTask(packetChannel, tasksHandler, id)

    override def requestFileInformation(owner: InetSocketAddress, path: String): Task[TransferableFile] =
        new FileInfoTask(packetChannel, tasksHandler, owner, path)

    override def start(): Unit = new Thread(() => {
        println("ready !")

        val buffer = ByteBuffer.allocate(Constants.MAX_PACKET_LENGTH)
        //enable the task management
        tasksHandler.start()
        while (true) {
            updateNetwork(buffer)
        }
    }).start()

    override def close(): Unit = {
        socketChannel.close()
    }

    def updateNetwork(buffer: ByteBuffer): Unit = {
        socketChannel.read(buffer)
        val packet = Protocol.toPacket(buffer)
        if (tasksHandler.handlePacket(packet, completerFactory, packetChannel)) {
            return
        }
        packetChannel.addPacket(packet)
        buffer.clear()
    }

        def configSocket(): SocketChannel = {
            println("connecting to server...")
            val socket = SocketChannel.open(serverAddress)
            println("connected !")
            socket.configureBlocking(true)
            socket
        }

    }