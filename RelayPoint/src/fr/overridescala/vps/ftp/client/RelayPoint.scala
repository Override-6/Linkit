package fr.overridescala.vps.ftp.client

import java.net.{InetSocketAddress, Socket}
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset
import java.nio.file.Paths

import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.packet.ext.fundamental.{ErrorPacket, SystemPacket, TaskInitPacket}
import fr.overridescala.vps.ftp.api.task.ext.TaskLoader
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.api.{Relay, RelayProperties}
import fr.overridescala.vps.ftp.client.tasks.ClientExtension

import scala.util.control.NonFatal

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String,
                 localRun: Boolean) extends Relay {

    @volatile private var open = false
    private val socket = new Socket(serverAddress.getAddress, serverAddress.getPort)

    private val taskFolderPath =
        if (localRun) Paths.get("C:\\Users\\maxim\\Desktop\\Dev\\VPS\\modules\\Tasks")
        else Paths.get("Tasks").toRealPath()

    override val taskLoader = new TaskLoader(this, taskFolderPath)
    override val packetManager = new PacketManager()
    private val tasksHandler = new ClientTasksHandler(socket, this)

    override val taskCompleterHandler: TaskCompleterHandler = tasksHandler.tasksCompleterHandler
    private val packetReader = new PacketReader(socket)

    override val properties = new RelayProperties
    private val channelCache = new PacketChannelManagerCache

    private implicit val systemChannel: PacketChannel.Async = createAsyncChannel(Constants.SERVER_ID, -2)

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        task.preInit(tasksHandler, identifier)
        RelayTaskAction(task)
    }


    override def start(): Unit = {
        val thread = new Thread(() => {
            println("Ready !")
            println("Current encoding is " + Charset.defaultCharset().name())
            println("Listening on port " + Constants.PORT)
            println("Computer name is " + System.getenv().get("COMPUTERNAME"))

            //enable the task management
            tasksHandler.start()
            taskLoader.refreshTasks()
            AsyncPacketChannel.launchThreadIfNot(packetManager)

            open = true
            while (open) {
                try {
                    val bytes = packetReader.readNextPacketBytes()
                    if (bytes == null)
                        return
                    val packet = packetManager.toPacket(bytes)
                    handlePacket(packet)
                } catch {
                    case _: AsynchronousCloseException =>
                        Console.err.println("asynchronous close.")
                        close()
                    case NonFatal(e) =>
                        e.printStackTrace()
                        Console.err.println(s"suddenly disconnected from the server")
                        close()
                }
            }
        })
        thread.setName("RelayPoint Packet handling")
        thread.start()
    }

    def handlePacket(packet: Packet): Unit = packet match {
        case init: TaskInitPacket => tasksHandler.handlePacket(init)
        case error: ErrorPacket if error.errorType == ErrorPacket.ABORT_TASK => tasksHandler.skipCurrent()
        case _: Packet => channelCache.injectPacket(packet)
    }


    override def close(): Unit = {
        open = false
        systemChannel.sendPacket(SystemPacket("DISCONNECT"))
        socket.close()
        tasksHandler.close()
        packetReader.close()
    }

    override def createSyncChannel(linkedRelayID: String, id: Int): PacketChannel.Sync = {
        createSyncChannel0(linkedRelayID, id)
    }

    override def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async = {
        new AsyncPacketChannel(identifier, linkedRelayID, id, channelCache, socket)
    }

    private[client] def createSyncChannel0(linkedRelayID: String, id: Int): SyncPacketChannel = {
        new SyncPacketChannel(socket, linkedRelayID, identifier, id, channelCache, packetManager)
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new UnsupportedOperationException("Relay Point have to be started !")
    }

    //initial tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close()))
    new ClientExtension(this).main() //manually adds local / private Task extension
}