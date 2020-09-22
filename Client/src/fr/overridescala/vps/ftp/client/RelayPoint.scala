package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.Charset

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.{PacketLoader, Protocol}
import fr.overridescala.vps.ftp.api.task.{TaskAction, TaskCompleterFactory, TaskConcoctor}
import fr.overridescala.vps.ftp.api.utils.Constants

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String) extends Relay {

    val buffer: ByteBuffer = ByteBuffer.allocateDirect(Constants.MAX_PACKET_LENGTH)

    private val socket = configSocket()
    private val tasksHandler = new ClientTasksHandler(socket)
    private val completerFactory = new RelayPointTaskCompleterFactory(tasksHandler)
    private val packetLoader = new PacketLoader()

    @volatile private var open = false

    override def scheduleTask[R, T >: TaskAction[R]](concoctor: TaskConcoctor[R, TaskAction[R]]): TaskAction[R] = {
        concoctor.concoct(tasksHandler)
    }

    override def getCompleterFactory: TaskCompleterFactory = completerFactory

    override def start(): Unit = {
        val thread = new Thread(() => {
            println("ready !")
            println("current encoding is " + Charset.defaultCharset().name())
            println("listening on port " + Constants.PORT)
            //enable the task management
            tasksHandler.start()

            open = true
            while (open) {
                try {
                    updateNetwork()
                } catch {
                    case e: Throwable =>
                        e.printStackTrace()
                        Console.err.println("suddenly disconnected from the server.")
                        close()
                }
            }
        })
        thread.setName("RelayPoint")
        thread.start()
    }

    override def close(): Unit = {
        open = false
        socket.close()
    }

    private def updateNetwork(): Unit = synchronized {
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

    private def configSocket(): SocketChannel = {
        println("connecting to server...")
        val socket = SocketChannel.open(serverAddress)
        println("connected !")
        socket.configureBlocking(true)
        socket
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new UnsupportedOperationException("Relay Point have to be started !")
    }

    //initial tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close()))
    socket.write(Protocol.createTaskPacket(-1, "INIT", identifier.getBytes))

}