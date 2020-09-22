package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.Charset

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.{PacketLoader, Protocol, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.task.tasks._
import fr.overridescala.vps.ftp.api.task.{Task, TaskAction}
import fr.overridescala.vps.ftp.api.transfer.{FileDescription, TransferDescription}
import fr.overridescala.vps.ftp.api.utils.Constants

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String) extends Relay {

    val buffer: ByteBuffer = ByteBuffer.allocateDirect(Constants.MAX_PACKET_LENGTH)

    private val socket = configSocket()
    private val tasksHandler = new ClientTasksHandler(socket)
    private val completerFactory = new RelayPointTaskCompleterFactory(tasksHandler)
    private val packetLoader = new PacketLoader()

    override def doDownload(description: TransferDescription): Task[Unit] =
        new DownloadTask(tasksHandler, description)

    override def doUpload(description: TransferDescription): Task[Unit] =
        new UploadTask(tasksHandler, description)

    override def requestFileInformation(ownerID: String, path: String): Task[FileDescription] =
        new FileInfoTask(tasksHandler, ownerID, path)

    override def requestCreateFile(ownerID: String, path: String): TaskAction[Unit] = {
        new CreateFileTask(path, ownerID, tasksHandler)
    }

    override def start(): Unit = {
        val thread = new Thread(() => {
            println("ready !")
            println("current encoding is " + Charset.defaultCharset().name())
            println("listening on port " + Constants.PORT)
            //enable the task management
            tasksHandler.start()
            while (true) {
                try {
                    updateNetwork()
                } catch {
                    case e: Throwable => {
                        e.printStackTrace()
                        Console.err.println("suddenly disconnected from the server.")
                        return
                    }
                }
            }
        })
        thread.setName("RelayPoint")
        thread.start()
    }

    override def close(): Unit = {
        socket.close()
    }

    def updateNetwork(): Unit = synchronized {
        val count = socket.read(buffer)
        if (count < 1)
            return

        val bytes = new Array[Byte](count)
        buffer.flip()
        buffer.get(bytes)

        packetLoader.add(bytes)

        var packet = packetLoader.nextPacket
        while (packet != null) {
            tasksHandler.handlePacket(packet, completerFactory, identifier, socket)
            packet = packetLoader.nextPacket
        }

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
    socket.write(Protocol.createTaskPacket(-1, "INIT", identifier.getBytes))
    start()
    new StressTestTask(tasksHandler, 100000000)
            .complete()

}