package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousCloseException, SocketChannel}
import java.nio.charset.Charset

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.exceptions.TaskException
import fr.overridescala.vps.ftp.api.packet.{PacketLoader, Protocol}
import fr.overridescala.vps.ftp.api.task.{TaskAction, TaskCompleterHandler, TaskConcoctor}
import fr.overridescala.vps.ftp.api.utils.{Constants, PerformanceMeter}

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String) extends Relay {

    val buffer: ByteBuffer = ByteBuffer.allocateDirect(Constants.MAX_PACKET_LENGTH)

    private val socket = configSocket()
    private val tasksHandler = new ClientTasksHandler(socket, this)
    private val packetLoader = new PacketLoader()


    @volatile private var open = false

    override def scheduleTask[R, T >: TaskAction[R]](concoctor: TaskConcoctor[R]): RelayTaskAction[R] = {
        ensureOpen()
        val taskAction = concoctor.concoct(tasksHandler)
        RelayTaskAction(taskAction)
    }

    override def getCompleterFactory: TaskCompleterHandler = tasksHandler.getTasksCompleterHandler

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
                    val t0 = System.currentTimeMillis()
                    updateNetwork()
                    val t1 = System.currentTimeMillis()
                    println(s"time to update ${t1 - t0}")
                } catch {
                    case _: AsynchronousCloseException => Console.err.println("asynchronous close.")
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
        val meter = new PerformanceMeter
        val count = socket.read(buffer)
        if (count < 1)
            return
        meter.printPerf("reading")
        val bytes = new Array[Byte](count)
        buffer.flip()
        buffer.get(bytes)

        packetLoader.add(bytes)

        meter.printPerf("adding packet")
        var packet = packetLoader.nextPacket
        while (packet != null) {
            meter.printPerf("getting next packet")
            tasksHandler.handlePacket(packet, identifier, socket)
            meter.printPerf("handling packet")
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

}