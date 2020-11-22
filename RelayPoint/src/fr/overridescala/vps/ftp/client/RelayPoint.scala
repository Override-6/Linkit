package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}

import fr.overridescala.vps.ftp.api.`extension`.RelayExtensionLoader
import fr.overridescala.vps.ftp.api.exceptions.{PacketException, RelayClosedException, RelayException, RelayInitialisationException}
import fr.overridescala.vps.ftp.api.packet.{PacketManager, _}
import fr.overridescala.vps.ftp.api.packet.fundamental._
import fr.overridescala.vps.ftp.api.system.event.EventDispatcher
import fr.overridescala.vps.ftp.api.system.{Reason, SystemOrder, SystemPacket, SystemPacketChannel}
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.vps.ftp.api.{Relay, RelayProperties}
import fr.overridescala.vps.ftp.client.RelayPoint.{Port, ServerID}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String, loadTasks: Boolean) extends Relay {

    override val eventDispatcher: EventDispatcher = new EventDispatcher
    private val notifier = eventDispatcher.notifier

    @volatile private var open = false
    private val socket = new ClientDynamicSocket(serverAddress, notifier)

    private val extensionFolderPath = getExtensionFolderPath
    override val extensionLoader = new RelayExtensionLoader(this, extensionFolderPath)
    override val properties = new RelayProperties

    override val packetManager = new PacketManager(notifier)
    private val packetReader = new PacketReader(socket)

    private val channelsHandler = new PacketChannelsHandler(notifier, socket, packetManager)
    private implicit val systemChannel: SystemPacketChannel = new SystemPacketChannel(ServerID, identifier, channelsHandler)

    private val tasksHandler = new ClientTasksHandler(systemChannel, this)
    override val taskCompleterHandler: TaskCompleterHandler = tasksHandler.tasksCompleterHandler


    override def start(): Unit = {
        println("Current encoding is " + Charset.defaultCharset().name())
        println("Listening on port " + Port)
        println("Computer name is " + System.getenv().get("COMPUTERNAME"))

        loadLocal()
        startPacketThreadListener()
        loadRemote()

        println("Ready !")
        notifier.onReady()
    }

    private def startPacketThreadListener(): Unit = {
        val thread = new Thread(() => {
            open = true
            while (open && socket.isOpen)
                listen()
        })
        thread.setName("RelayPoint Packet handling")
        thread.start()
    }

    private def loadRemote(): Unit = {
        println(s"Connecting to server with identifier '$identifier'...")
        socket.start()
        val response = systemChannel.nextPacketAsP(): DataPacket
        if (response.header == "ERROR")
            throw RelayInitialisationException(s"Another relay point with id '$identifier' is currently connected on the targeted network.")

        println("Connected !")
    }

    private def loadLocal() : Unit = {
        println("Loading tasks handler...")
        tasksHandler.start()
        if (loadTasks) {
            println("Loading Relay extensions from folder " + extensionFolderPath)
            extensionLoader.loadExtensions()
        }
        AsyncPacketChannel.UploadThread.start()
        println("Async Upload Thread started !")
    }

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        ensureTargetExists(task.targetID)

        task.preInit(tasksHandler, identifier)
        notifier.onTaskScheduled(task)
        RelayTaskAction.of(task)
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
        new AsyncPacketChannel(identifier, linkedRelayID, id, channelsHandler)
    }

    def isConnected: Boolean = socket.isConnected

    private[client] def createSyncChannel0(linkedRelayID: String, id: Int): SyncPacketChannel = {
        new SyncPacketChannel(linkedRelayID, identifier, id, channelsHandler)
    }

    private def close(relayId: String, reason: Reason): Unit = {
        if (!open)
            return //already closed.
        if (socket.isConnected && reason.isInternal) {
            systemChannel.sendPacket(SystemPacket(SystemOrder.CLIENT_CLOSE, reason))
        }

        systemChannel.close(reason)
        socket.close(reason)
        tasksHandler.close(reason)
        channelsHandler.close(reason)

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
            notifier.onPacketReceived(packet)
            handlePacket(packet)
        }
        catch {
            case _: AsynchronousCloseException =>
                Console.err.println("Asynchronous close.")
                close(Reason.INTERNAL_ERROR)
            case e: PacketException =>
                Console.err.println(e.getMessage)
                close(Reason.INTERNAL_ERROR)
            case NonFatal(e) =>
                e.printStackTrace()
                Console.err.println(s"Suddenly disconnected from the server")
                close(Reason.INTERNAL_ERROR)
        }
    }

    private def handlePacket(packet: Packet): Unit = packet match {
        case init: TaskInitPacket => tasksHandler.handlePacket(init)
        case system: SystemPacket => handleSystemPacket(system)
        case _: Packet => channelsHandler.injectPacket(packet)
    }


    private def handleSystemPacket(system: SystemPacket): Unit = {
        val order = system.order
        val reason = system.reason.reversed()
        val origin = system.senderID

        println(s"Received system order $order from ${origin}")
        notifier.onSystemOrderReceived(order, reason)
        order match {
            case SystemOrder.CLIENT_CLOSE => close(origin, reason)
            case SystemOrder.GET_IDENTIFIER => systemChannel.sendPacket(DataPacket(identifier))
            case SystemOrder.ABORT_TASK => tasksHandler.skipCurrent(reason)
            case _@(SystemOrder.SERVER_CLOSE | SystemOrder.CHECK_ID) => sendErrorPacket(order, "Received forbidden order.")
            case _ => sendErrorPacket(order, "Unknown order.")
        }

        def sendErrorPacket(order: SystemOrder, cause: String): Unit = {
            val error = ErrorPacket("SystemError",
                s"System packet order '$order' couldn't be handled by this RelayPoint.",
                cause)
            systemChannel.sendPacket(error)
            error.printError()
        }
    }

    private def getExtensionFolderPath: Path = {
        val path = System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\RelayExtensions"
            case _ => "RelayExtensions/"
        }
        Paths.get(path)
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new RelayClosedException("Relay Point have to be started !")
    }


    private def ensureTargetExists(targetID: String): Unit = {
        systemChannel.sendOrder(SystemOrder.CHECK_ID, Reason.INTERNAL, targetID.getBytes)
        val response = (systemChannel.nextPacketAsP(): DataPacket).header
        if (response == "ERROR")
            throw new RelayException(s"Target '$targetID' does not exists !")
    }

    //initial tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close(Reason.INTERNAL)))
}

object RelayPoint {
    val ServerID = "server"
    val Port = 48484
}