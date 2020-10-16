package fr.overridescala.vps.ftp.client

import java.net.{InetSocketAddress, Socket}
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset
import java.nio.file.Path

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.packet.PacketReader
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.task.ext.TaskLoader
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.client.tasks.ClientExtension

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String) extends Relay {

    private val socket = new Socket(serverAddress.getAddress, serverAddress.getPort)
    private val tasksHandler = new ClientTasksHandler(socket, this)
    @volatile private var open = false

    override val taskLoader = new TaskLoader(this, Path.of("C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\Tasks"))
    override val packetManager = new PacketManager()
    private val packetReader = new PacketReader(socket, packetManager)

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        task.preInit(tasksHandler, identifier)
        RelayTaskAction(task)
    }

    override val taskCompleterHandler: TaskCompleterHandler = tasksHandler.tasksCompleterHandler

    override def start(): Unit = {
        val thread = new Thread(() => {
            println("ready !")
            println("current encoding is " + Charset.defaultCharset().name())
            println("listening on port " + Constants.PORT)
            println("computer name is " + System.getenv().get("COMPUTERNAME"))

            //enable the task management
            tasksHandler.start()
            taskLoader.refreshTasks()

            open = true
            while (open) {
                try {
                    packetReader
                            .readPacket()
                            .ifPresent(p => tasksHandler.handlePacket(p))
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
    new ClientExtension(this).main() //manually adds local / private Task extension

}