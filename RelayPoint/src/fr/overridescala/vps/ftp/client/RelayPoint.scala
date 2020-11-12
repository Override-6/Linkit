package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset
import java.nio.file.Paths

import fr.overridescala.vps.ftp.api.exceptions.RelayInitialisationException
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.packet.ext.fundamental._
import fr.overridescala.vps.ftp.api.task.ext.TaskLoader
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.api.{Relay, RelayProperties}
import javafx.application.Platform

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String,
                 localRun: Boolean) extends Relay {

    @volatile private var open = false
    private val socket = new ClientDynamicSocket(serverAddress)

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

    private implicit val systemChannel: PacketChannel.Sync = createSyncChannel0(Constants.SERVER_ID, -6)

    override def start(): Unit = {
        val thread = new Thread(() => {
            println("Current encoding is " + Charset.defaultCharset().name())
            println("Listening on port " + Constants.PORT)
            println("Computer name is " + System.getenv().get("COMPUTERNAME"))

            //enable the task management
            tasksHandler.start()
            taskLoader.refreshTasks()
            AsyncPacketChannel.launchThreadIfNot(packetManager)

            println("Ready !")
            open = true
            while (open)
                listen()
        })
        thread.setName("RelayPoint Packet handling")
        thread.start()
        /*//FIX ME delete
        new Thread(() => {
            Console.err.println("A Connection loss will be simulated by closing socket in 10 seconds...")
            Thread.sleep(100000)
            println("Closing socket...")
            socket.close()
        }).start()*/

    }

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        task.preInit(tasksHandler, identifier)
        RelayTaskAction(task)
    }

    override def close(): Unit =
        closeConnection(true)


    override def createSyncChannel(linkedRelayID: String, id: Int): PacketChannel.Sync = {
        ensureOpen()
        createSyncChannel0(linkedRelayID, id)
    }

    override def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async = {
        ensureOpen()
        new AsyncPacketChannel(identifier, linkedRelayID, id, channelCache, socket)
    }

    private[client] def createSyncChannel0(linkedRelayID: String, id: Int): SyncPacketChannel = {
        new SyncPacketChannel(socket, linkedRelayID, identifier, id, channelCache, packetManager)
    }


    private def listen(): Unit = {
        try {
            val bytes = packetReader.readNextPacketBytes()
            if (bytes == null)
                return
            val packet = packetManager.toPacket(bytes)
            handlePacket(packet)
        }
        catch {
            case _: AsynchronousCloseException =>
                Console.err.println("Asynchronous close.")
                close()
            case NonFatal(e) =>
                e.printStackTrace()
                Console.err.println(s"Suddenly disconnected from the server")
                close()
        }
    }

    private def handlePacket(packet: Packet): Unit = packet match {
        case init: TaskInitPacket => tasksHandler.handlePacket(init)
        case error: ErrorPacket if error.errorType == ErrorPacket.ABORT_TASK => tasksHandler.skipCurrent()
        case system: SystemPacket => handleSystemPacket(system)
        case _: Packet => channelCache.injectPacket(packet)
    }


    private def handleSystemPacket(system: SystemPacket): Unit = {
        val order = system.order
        println(s"Received system order $order from ${system.senderID}")
        order match {
            case SystemPacket.ClientClose => closeConnection(false)
            case SystemPacket.ServerClose => systemChannel.sendPacket(createSystemErrorPacket(order, "Received forbidden order."))
            case SystemPacket.ClientInitialisation => Future(initToServer())
            case _ =>
                systemChannel.sendPacket(createSystemErrorPacket(order, "Unknown order."))
        }
    }

    private def createSystemErrorPacket(order: String, cause: String) =
        ErrorPacket("SystemError",
            s"System packet order '$order' couldn't be handled by this RelayPoint.",
            cause)

    private def closeConnection(requestIsLocal: Boolean): Unit = {
        if (requestIsLocal) {
            systemChannel.sendPacket(SystemPacket(SystemPacket.ClientClose))
            systemChannel.nextPacket() //wait an empty packet from server in order to sync the disconnection
        } else systemChannel.sendPacket(EmptyPacket()) //Notifies the server in order to sync the disconnection

        socket.close()
        tasksHandler.close()
        open = false
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new UnsupportedOperationException("Relay Point have to be started !")
    }

    private def initToServer(): Unit = {
        systemChannel.sendPacket(DataPacket(identifier))
        val response: DataPacket = systemChannel.nextPacketAsP()

        if (response.header == "ERROR")
            throw RelayInitialisationException(s"Another relay point with id '$identifier' is currently connected on the targeted network")

        println("Successfully connected to the server !")
    }

    //initial tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close()))
}