package fr.overridescala.linkkit.client

import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}

import fr.overridescala.linkkit.api.{Relay, exceptions}
import fr.overridescala.linkkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.overridescala.linkkit.api.exceptions._
import fr.overridescala.linkkit.api.packet.channel.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.overridescala.linkkit.api.packet.collector.{AsyncPacketCollector, PacketCollector, SyncPacketCollector}
import fr.overridescala.linkkit.api.packet.fundamental._
import fr.overridescala.linkkit.api.packet.{PacketManager, _}
import fr.overridescala.linkkit.api.system._
import fr.overridescala.linkkit.api.system.event.EventObserver
import fr.overridescala.linkkit.api.system.security.RelaySecurityManager
import fr.overridescala.linkkit.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.linkkit.client.RelayPoint.ServerID
import fr.overridescala.linkkit.client.config.RelayPointConfiguration

import scala.util.control.NonFatal

object RelayPoint {
    val version: Version = Version("RelayPoint", "0.5.2", stable = false)

    val ServerID = "server"
}

class RelayPoint(override val configuration: RelayPointConfiguration) extends Relay {

    override val eventObserver: EventObserver = new EventObserver(configuration.enableEventHandling)
    private val notifier = eventObserver.notifier

    @volatile private var open = false
    private val socket = new ClientDynamicSocket(configuration.serverAddress, notifier, configuration.reconnectionPeriod)

    @volatile private var serverErrConsole: RemoteConsole.Err = _ //affected once Relay initialised

    override val packetManager = new PacketManager(this)

    override val extensionLoader = new RelayExtensionLoader(this)
    override val properties = new RelayProperties

    private val traffic = new SimpleTrafficHandler(this, socket)
    private implicit val systemChannel: SystemPacketChannel = new SystemPacketChannel(ServerID, traffic)

    private val tasksHandler = new ClientTasksHandler(systemChannel, this)
    override val taskCompleterHandler: TaskCompleterHandler = tasksHandler.tasksCompleterHandler
    private val remoteConsoles = new RemoteConsolesHandler(this)

    override val relayVersion: Version = RelayPoint.version

    override val securityManager: RelaySecurityManager = configuration.securityManager

    override def start(): Unit = {
        println("Current encoding is " + Charset.defaultCharset().name())
        println("Listening on port " + configuration.serverAddress.getPort)
        println("Computer name is " + System.getenv().get("COMPUTERNAME"))
        println(relayVersion)
        println(apiVersion)

        try {
            securityManager.checkRelay(this)

            loadLocal()
            startPacketThreadListener()
            loadRemote()

            securityManager.checkRelay(this)
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                close(Reason.INTERNAL_ERROR)
        }

        println("Ready !")
        notifier.onReady()
    }

    private def startPacketThreadListener(): Unit = {
        val thread = new Thread(() => {
            val packetReader = new PacketReader(socket, securityManager, serverErrConsole)
            open = true
            while (open && socket.isOpen)
                listen(packetReader)
            open = false
        })
        thread.setName("RelayPoint Packet Handling")
        thread.start()
    }

    private def loadRemote(): Unit = {
        println(s"Connecting to server with identifier '$identifier'...")
        socket.start()

        val response = systemChannel.nextPacketAsP(): DataPacket
        if (response.header == "ERROR")
            throw RelayInitialisationException(new String(response.content))

        val outOpt = getConsoleOut(ServerID)
        val errOpt = getConsoleErr(ServerID)
        if (outOpt.isEmpty || errOpt.isEmpty)
            throw RelayInitialisationException("Could not retrieve remote console of server")
        serverErrConsole = errOpt.get

        systemChannel.sendOrder(SystemOrder.PRINT_INFO, Reason.INTERNAL)
        println("Connected !")

    }

    private def loadLocal(): Unit = {
        if (configuration.enableTasks) {
            println("Loading tasks handler...")
            tasksHandler.start()
        }
        if (configuration.enableExtensionsFolderLoad) {
            println("Loading Relay extensions from folder " + configuration.extensionsFolder)
            extensionLoader.loadExtensions()
        }
    }

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        ensureTargetValid(task.targetID)
        if (!configuration.enableTasks)
            throw new TaskException("Task handling is disabled according to RelayConfiguration")

        task.preInit(tasksHandler)
        notifier.onTaskScheduled(task)
        RelayTaskAction.of(task)
    }

    override def close(reason: Reason): Unit = {
        close(identifier, reason)
    }

    override def createSyncChannel(linkedRelayID: String, id: Int, cacheSize: Int): PacketChannel.Sync = {
        new SyncPacketChannel(linkedRelayID, id, cacheSize, traffic)
    }

    override def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async = {
        new AsyncPacketChannel(linkedRelayID, id, traffic)
    }

    override def createAsyncCollector(id: Int): PacketCollector.Async = {
        new AsyncPacketCollector(traffic, id)
    }

    override def createSyncCollector(id: Int, cacheSize: Int): PacketCollector.Sync = {
        new SyncPacketCollector(traffic, cacheSize, id)
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

        extensionLoader.close()
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

            if (configuration.checkReceivedPacketTargetID)
                checkPacket(coordinates)

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

    private def checkPacket(coordinates: PacketCoordinates): Unit = {
        val targetID = coordinates.targetID
        if (targetID == identifier || targetID == "unknown")
            return

        val sender = coordinates.senderID
        val optErr = getConsoleErr(sender)
        val msg = s"Could not handle packet : targetID ($targetID) isn't equals to this relay identifier !"
        if (optErr.isDefined)
            optErr.get.println(msg)
        throw new UnexpectedPacketException(msg)
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

        notifier.onSystemOrderReceived(order, reason)

        import SystemOrder._
        order match {
            case CLIENT_CLOSE => close(origin, reason)
            case GET_IDENTIFIER => systemChannel.sendPacket(DataPacket(identifier))
            case ABORT_TASK => tasksHandler.skipCurrent(reason)
            case PRINT_INFO => getConsoleOut(origin).orNull.println(s"$relayVersion ($apiVersion)")

            case _@(SERVER_CLOSE | CHECK_ID) => sendErrorPacket(order, "Received forbidden order.")
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

    private def ensureOpen(): Unit = {
        if (!open)
            throw new RelayCloseException("Relay Point have to be started !")
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