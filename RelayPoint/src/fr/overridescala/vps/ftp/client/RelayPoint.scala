package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset
import java.nio.file.Paths

import fr.overridescala.vps.ftp.api.`extension`.RelayExtensionLoader
import fr.overridescala.vps.ftp.api.`extension`.event.EventDispatcher
import fr.overridescala.vps.ftp.api.`extension`.packet.PacketManager
import fr.overridescala.vps.ftp.api.exceptions.{PacketException, RelayInitialisationException}
import fr.overridescala.vps.ftp.api.packet._
import fr.overridescala.vps.ftp.api.packet.fundamental._
import fr.overridescala.vps.ftp.api.system.{Reason, SystemPacket}
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.api.{Relay, RelayProperties}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String,
                 localRun: Boolean, loadTasks: Boolean) extends Relay {

    @volatile private var open = false

    override val eventDispatcher: EventDispatcher = new EventDispatcher
    private val notifier = eventDispatcher.notifier
    private val socket = new ClientDynamicSocket(serverAddress, notifier)

    private val taskFolderPath =
        if (localRun) Paths.get("C:\\Users\\maxim\\Desktop\\Dev\\VPS\\modules\\RelayExtensions")
        else Paths.get("RelayExtensions")

    override val extensionLoader = new RelayExtensionLoader(this, taskFolderPath)
    override val packetManager = new PacketManager(notifier)
    private val tasksHandler = new ClientTasksHandler(socket, this)

    override val taskCompleterHandler: TaskCompleterHandler = tasksHandler.tasksCompleterHandler
    private val packetReader = new PacketReader(socket)

    override val properties = new RelayProperties
    private val channelCache = new PacketChannelsHandler(notifier)

    private implicit val systemChannel: PacketChannel.Sync = createSyncChannel0(Constants.SERVER_ID, -6)
    private val lock: Object = new Object

    override def start(): Unit = {
        val thread = new Thread(() => {
            println("Current encoding is " + Charset.defaultCharset().name())
            println("Listening on port " + Constants.PORT)
            println("Computer name is " + System.getenv().get("COMPUTERNAME"))

            //enable the task management
            println("Starting tasks handler...")
            tasksHandler.start()
            if (loadTasks) {
                println("Loading Relay extensions from folder " + taskFolderPath)
                extensionLoader.loadExtensions()
            }
            AsyncPacketChannel.launch(packetManager)
            println(s"Connecting to server with identifier '$identifier'...")
            socket.start()
            println("Connected !")

            println("Ready !")
            notifier.onReady()
            lock.synchronized {
                lock.notifyAll()
            }

            open = true
            while (open && socket.isOpen)
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
        notifier.onTaskScheduled(task)
        RelayTaskAction(task)
    }

    override def close(reason: Reason): Unit = {
        close(identifier, reason)
    }


    override def createSyncChannel(linkedRelayID: String, id: Int): PacketChannel.Sync = {
        ensureOpen()
        createSyncChannel0(linkedRelayID, id)
    }

    override def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async = {
        ensureOpen()
        new AsyncPacketChannel(identifier, linkedRelayID, id, channelCache, socket)
    }

    def isConnected: Boolean = socket.isConnected

    def awaitStart(): Unit = lock.synchronized {
        if (!isConnected)
            lock.wait()
    }

    private[client] def createSyncChannel0(linkedRelayID: String, id: Int): SyncPacketChannel = {
        new SyncPacketChannel(socket, linkedRelayID, identifier, id, channelCache, packetManager)
    }

    private def close(relayId: String, reason: Reason): Unit = {
        if (reason != Reason.EXTERNAL_REQUEST) {
            systemChannel.sendPacket(SystemPacket(SystemPacket.ClientClose))
        } else {
            systemChannel.sendPacket(EmptyPacket())
            systemChannel.nextPacket() //wait an empty packet from server in order to sync the disconnection
        } //Notifies the server in order to sync the disconnection

        println("closing socket...")
        socket.close(reason)
        println("socket closed !")
        tasksHandler.close(reason)
        open = false
        notifier.onClosed(relayId, reason)
        println("closed !")
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
                close(Reason.ERROR_OCCURRED)
            case e: PacketException =>
                Console.err.println(e.getMessage)
                notifier.onSystemError(e)
                close(Reason.ERROR_OCCURRED)
            case NonFatal(e) =>
                e.printStackTrace()
                Console.err.println(s"Suddenly disconnected from the server")
                close(Reason.ERROR_OCCURRED)
        }
    }

    private def handlePacket(packet: Packet): Unit = packet match {
        case init: TaskInitPacket => tasksHandler.handlePacket(init)
        case error: ErrorPacket if error.errorType == ErrorPacket.ABORT_TASK => tasksHandler.skipCurrent(Reason.EXTERNAL_REQUEST)
        case system: SystemPacket => handleSystemPacket(system)
        case _: Packet => channelCache.injectPacket(packet)
    }


    private def handleSystemPacket(system: SystemPacket): Unit = {
        val order = system.infoKind
        println(s"Received system order $order from ${system.senderID}")
        order match {
            case SystemPacket.ClientClose => close(Reason.EXTERNAL_REQUEST)
            case SystemPacket.ServerClose => systemChannel.sendPacket(createSystemErrorPacket(order, "Received forbidden order."))
            case SystemPacket.ClientInitialisation => Future(initToServer())
            case _ => systemChannel.sendPacket(createSystemErrorPacket(order, "Unknown order."))
        }

        def createSystemErrorPacket(order: String, cause: String) =
            ErrorPacket("SystemError",
                s"System packet order '$order' couldn't be handled by this RelayPoint.",
                cause)
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
    Runtime.getRuntime.addShutdownHook(new Thread(() => close(Reason.LOCAL_REQUEST)))
}