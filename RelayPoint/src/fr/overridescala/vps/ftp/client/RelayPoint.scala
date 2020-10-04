package fr.overridescala.vps.ftp.client

import java.net.{InetSocketAddress, Socket}
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.PacketReader
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.vps.ftp.api.utils.Constants

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String) extends Relay {

    private val socket = new Socket(serverAddress.getAddress, serverAddress.getPort)
    socket.setPerformancePreferences(0, 0, Integer.MAX_VALUE)
    private val tasksHandler = new ClientTasksHandler(socket, this)
    private val packetReader = new PacketReader(socket)


    @volatile private var open = false

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        task.init(tasksHandler)
        RelayTaskAction(task)
    }

    override def getTaskCompleterHandler: TaskCompleterHandler = tasksHandler.tasksCompleterHandler

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
                    tasksHandler.handlePacket(packetReader.readPacket())
                } catch {
                    case _: AsynchronousCloseException =>
                        Console.err.println("asynchronous close.")
                        close()
                    case e: Throwable =>
                        e.printStackTrace()
                        Console.err.println("suddenly disconnected from the server.")
                        close()
                }
            }
        })
        thread.setName("RelayPoint Packet handling")
        thread.start()
    }

    override def close(): Unit = {
        open = false
        socket.close()
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new UnsupportedOperationException("Relay Point have to be started !")
    }

    //initial tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close()))

}