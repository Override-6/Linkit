package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.{Protocol, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks._
import fr.overridescala.vps.ftp.api.task.{Task, TasksHandler}
import fr.overridescala.vps.ftp.api.transfer.{TransferDescription, TransferableFile}
import fr.overridescala.vps.ftp.api.utils.Constants

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String) extends Relay {


    private val socketChannel = configSocket()
    private val tasksHandler = new TasksHandler()
    private val packetChannel = new SimplePacketChannel(socketChannel, tasksHandler)
    private val completerFactory = new RelayPointTaskCompleterFactory(tasksHandler)

    override def doDownload(description: TransferDescription): Task[Unit] =
        new DownloadTask(packetChannel, tasksHandler, description)

    override def doUpload(description: TransferDescription): Task[Unit] =
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
            try {
                updateNetwork(buffer)
            } catch {
                case e: Throwable => {
                    e.printStackTrace()
                    Console.err.println("suddenly disconnected from the server.")
                    return
                }
            }
        }
    }).start()

    override def close(): Unit = {
        socketChannel.close()
    }

    def updateNetwork(buffer: ByteBuffer): Unit = synchronized {
        val count = socketChannel.read(buffer)
        if (count < 1)
            return
        val bytes = new Array[Byte](count)
        buffer.flip()
        buffer.get(bytes)
        val packet = Protocol.toPacket(bytes)
        tasksHandler.handlePacket(packet, completerFactory, packetChannel)

        buffer.clear()
    }

    def configSocket(): SocketChannel = {
        println("connecting to server...")
        val socket = SocketChannel.open(serverAddress)
        println("connected !")
        socket.configureBlocking(true)
        socket
    }

    //initial tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close()))
    new InitTask(tasksHandler, packetChannel, identifier).queueWithError(msg => {
        Console.err.print(s"unable to connect to the server : $msg")
        close()
    })

}