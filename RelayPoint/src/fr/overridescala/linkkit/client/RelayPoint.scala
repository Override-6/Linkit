package fr.overridescala.linkkit.client

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}

import fr.overridescala.linkkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.overridescala.linkkit.api.exceptions.{RelayClosedException, RelayException, RelayInitialisationException}
import fr.overridescala.linkkit.api.packet.fundamental._
import fr.overridescala.linkkit.api.packet.{PacketManager, _}
import fr.overridescala.linkkit.api.system.event.EventObserver
import fr.overridescala.linkkit.api.system._
import fr.overridescala.linkkit.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.linkkit.api.Relay
import RelayPoint.{Port, ServerID}
import fr.overridescala.linkkit.api.packet.channel.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.overridescala.linkkit.api.packet.collector.{AsyncPacketCollector, PacketCollector, SyncPacketCollector}

import scala.util.control.NonFatal

object RelayPoint {
    val version: Version = Version("RelayPoint", "0.3.0", stable = false)

    val ServerID = "server"
    val Port = 48484
}
class RelayPoint(private val serverAddress: InetSocketAddress,
                 override val identifier: String, loadTasks: Boolean) extends Relay {

    override val eventObserver: EventObserver = new EventObserver
    private val notifier = eventObserver.notifier

    @volatile private var open = false
    private val socket = new ClientDynamicSocket(serverAddress, notifier)

    @volatile private var serverErrConsole: RemoteConsole.Err = _ //affected once Relay started

    override val packetManager = new PacketManager(notifier)

    private val extensionFolderPath = getExtensionFolderPath
    override val extensionLoader = new RelayExtensionLoader(this, extensionFolderPath)
    override val properties = new RelayProperties

    private val traffic = new SimpleTrafficHandler(notifier, socket, identifier, packetManager)
    private implicit val systemChannel: SystemPacketChannel = new SystemPacketChannel(ServerID, traffic)

    private val tasksHandler = new ClientTasksHandler(systemChannel, this)
    override val taskCompleterHandler: TaskCompleterHandler = tasksHandler.tasksCompleterHandler
    private val remoteConsoles = new RemoteConsolesHandler(this)

    override val relayVersion: Version = RelayPoint.version

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
            val packetReader = new PacketReader(socket, serverErrConsole)
            open = true
            while (open && socket.isOpen)
                listen(packetReader)
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

        serverErrConsole = getConsoleErr(ServerID).get

        println("Connected !")
    }

    private def loadLocal(): Unit = {
        println("Loading tasks handler...")
        tasksHandler.start()
        if (loadTasks) {
            println("Loading Relay extensions from folder " + extensionFolderPath)
            extensionLoader.loadExtensions()
        }
        println("Async Upload Thread started !")
    }

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        ensureTargetValid(task.targetID)

        task.preInit(tasksHandler)
        notifier.onTaskScheduled(task)
        RelayTaskAction.of(task)
    }

    override def close(reason: Reason): Unit = {
        close(identifier, reason)
    }


    override def createSyncChannel(linkedRelayID: String, id: Int): PacketChannel.Sync =  {
        ensureOpen()
        new SyncPacketChannel(linkedRelayID, id, traffic)
    }

    override def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async = {
        ensureOpen()
        new AsyncPacketChannel(linkedRelayID, id, traffic)
    }

    override def createAsyncCollector(id: Int): PacketCollector.Async = {
        new AsyncPacketCollector(traffic, id)
    }

    override def createSyncCollector(id: Int): PacketCollector.Sync = {
        new SyncPacketCollector(traffic, id)
    }

    override def getConsoleOut(targetId: String): Option[RemoteConsole] = Option(remoteConsoles.getOut(targetId))

    override def getConsoleErr(targetId: String): Option[RemoteConsole.Err] = Option(remoteConsoles.getErr(targetId))

    def isConnected: Boolean = socket.isConnected

    private def close(relayId: String, reason: Reason): Unit = {
        if (!open)
            return //already closed.
        if (socket.isConnected && reason.isInternal) {
            systemChannel.sendPacket(SystemPacket(SystemOrder.CLIENT_CLOSE, reason))
        }

        systemChannel.close(reason)
        socket.close(reason)
        tasksHandler.close(reason)
        traffic.close(reason)

        open = false
        notifier.onClosed(relayId, reason)
        println("closed !")
    }


    private def listen(reader: PacketReader): Unit = {
        try {
            val bytes = reader.readNextPacketBytes()
            if (bytes == null)
                return
            val (packet, coordinates) = packetManager.toPacket(bytes)
            notifier.onPacketReceived(packet, coordinates)
            handlePacket(packet, coordinates)
        }
        catch {
            case e: AsynchronousCloseException =>
                Console.err.println("Asynchronous close.")
                serverErrConsole.reportExceptionSimplified(e)
                close(Reason.INTERNAL_ERROR)

            case NonFatal(e) =>
                e.printStackTrace()

                Console.err.println(s"Suddenly disconnected from the server")
                serverErrConsole.reportExceptionSimplified(e)
                close(Reason.INTERNAL_ERROR)
        }
    }

    private def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = packet match {
        case init: TaskInitPacket => tasksHandler.handlePacket(init, coordinates)
        case system: SystemPacket => handleSystemPacket(system, coordinates)
        case _: Packet => traffic.injectPacket(packet, coordinates)
    }


    private def handleSystemPacket(system: SystemPacket, coords: PacketCoordinates): Unit = {
        val order = system.order
        val reason = system.reason.reversed()
        val origin = coords.senderID
        val content = system.content

        println(s"Received system order $order from $origin")
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
            case "LORDI-N4SO7IERS" => "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\FileTransferer\\ClientSide\\RelayExtensions"
            case _ => "RelayExtensions/"
        }
        Paths.get(path)
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new RelayClosedException("Relay Point have to be started !")
    }


    private def ensureTargetValid(targetID: String): Unit = {
        if (targetID == identifier)
            throw new RelayException("Can't start any task with oneself !")

        systemChannel.sendOrder(SystemOrder.CHECK_ID, Reason.INTERNAL, targetID.getBytes)
        val response = (systemChannel.nextPacketAsP(): DataPacket).header
        if (response == "ERROR")
            throw new RelayException(s"Target '$targetID' does not exists !")
    }

    //initial tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close(Reason.INTERNAL)))
}