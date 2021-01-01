package fr.`override`.linkit.client

import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.`override`.linkit.api.exception._
import fr.`override`.linkit.api.packet.channel.{AsyncPacketChannel, PacketChannel, SyncPacketChannel}
import fr.`override`.linkit.api.packet.collector.{AsyncPacketCollector, PacketCollector, SyncPacketCollector}
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.packet.{PacketManager, _}
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.system.event.EventObserver
import fr.`override`.linkit.api.network.ConnectionState
import fr.`override`.linkit.api.system.security.RelaySecurityManager
import fr.`override`.linkit.api.task.{Task, TaskCompleterHandler}
import fr.`override`.linkit.client.RelayPoint.ServerID
import fr.`override`.linkit.client.config.RelayPointConfiguration
import fr.`override`.linkit.client.network.PointNetwork

import scala.util.control.NonFatal

object RelayPoint {
    val version: Version = Version("RelayPoint", "0.9.0", stable = false)

    val ServerID = "server"
}

class RelayPoint private[client](override val configuration: RelayPointConfiguration) extends Relay {

    override val eventObserver: EventObserver = new EventObserver(configuration.enableEventHandling)
    private val notifier = eventObserver.notifier

    @volatile private var open = false
    private val socket = new ClientDynamicSocket(configuration.serverAddress, notifier, configuration.reconnectionPeriod)

    @volatile private var serverErrConsole: RemoteConsole.Err = _ //affected once Relay initialised

    override val packetManager = new PacketManager(this)

    override val extensionLoader = new RelayExtensionLoader(this)
    override val properties = new RelayProperties

    override val trafficHandler = new SimpleTrafficHandler(this, socket)
    implicit val systemChannel: SystemPacketChannel = new SystemPacketChannel(ServerID, trafficHandler)

    private val tasksHandler = new ClientTasksHandler(systemChannel, this)
    override val taskCompleterHandler: TaskCompleterHandler = tasksHandler.tasksCompleterHandler

    private val remoteConsoles = new RemoteConsolesContainer(this)

    override val relayVersion: Version = RelayPoint.version

    override val securityManager: RelaySecurityManager = configuration.securityManager

    override val network: PointNetwork = new PointNetwork(this)

    override def start(): Unit = {
        securityManager.checkRelay(this)

        println("Current encoding is " + Charset.defaultCharset().name())
        println("Listening on port " + configuration.serverAddress.getPort)
        println("Computer name is " + System.getenv().get("COMPUTERNAME"))
        println(relayVersion)
        println(Relay.ApiVersion)

        try {
            loadLocal()
            startPacketWorker()
            loadRemote()
        } catch {
            case NonFatal(e) =>
                e.printStackTrace()
                close(CloseReason.INTERNAL_ERROR)
        }
        securityManager.checkRelay(this)

        println("Ready !")
        notifier.onReady()
    }

    override def isConnected(identifier: String): Boolean = {
        systemChannel.sendOrder(SystemOrder.CHECK_ID, CloseReason.INTERNAL, identifier.getBytes)
        val response = systemChannel.nextPacket(DataPacket).header
        response == "OK"
    }

    override def close(reason: CloseReason): Unit = {
        close(identifier, reason)
    }

    override def addConnectionListener(action: ConnectionState => Unit): Unit = socket.addConnectionStateListener(action)

    override def getState: ConnectionState = socket.getState

    override def createSyncChannel(linkedRelayID: String, id: Int, cacheSize: Int): PacketChannel.Sync = {
        new SyncPacketChannel(linkedRelayID, id, cacheSize, trafficHandler)
    }

    override def createAsyncChannel(linkedRelayID: String, id: Int): PacketChannel.Async = {
        new AsyncPacketChannel(linkedRelayID, id, trafficHandler)
    }

    override def createAsyncCollector(id: Int): PacketCollector.Async = {
        new AsyncPacketCollector(trafficHandler, id)
    }

    override def createSyncCollector(id: Int, cacheSize: Int): PacketCollector.Sync = {
        new SyncPacketCollector(trafficHandler, cacheSize, id)
    }

    override def getConsoleOut(targetId: String): Option[RemoteConsole] = Option(remoteConsoles.getOut(targetId))

    override def getConsoleErr(targetId: String): Option[RemoteConsole.Err] = Option(remoteConsoles.getErr(targetId))

    def isConnected: Boolean = socket.getState == ConnectionState.CONNECTED

    private def startPacketWorker(): Unit = {
        val thread = new Thread(packetWorkerThreadGroup, () => {
            val packetReader = new PacketReader(socket, securityManager)
            socket.start()
            open = true
            while (open && socket.isOpen)
                listen(packetReader)
            open = false
        })
        thread.setName("RelayPoint Packet Worker")
        thread.start()
    }

    private def loadRemote(): Unit = {
        println(s"Connecting to server with relay id '$identifier'")
        val idLength = identifier.length
        socket.write(Array(idLength.toByte) ++ identifier.getBytes) //welcome packet

        val response = systemChannel.nextPacket(DataPacket)
        if (response.header == "ERROR")
            throw RelayInitialisationException(new String(response.content))

        val outOpt = getConsoleOut(ServerID)
        val errOpt = getConsoleErr(ServerID)

        if (outOpt.isEmpty || errOpt.isEmpty)
            throw RelayInitialisationException("Could not retrieve remote console of server")
        serverErrConsole = errOpt.get

        systemChannel.sendOrder(SystemOrder.PRINT_INFO, CloseReason.INTERNAL)
        println("Connected !")
        network.init()
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

    private def close(relayId: String, reason: CloseReason): Unit = {
        if (!open)
            return //already closed.

        if (reason.isInternal && isConnected) {
            systemChannel.sendPacket(SystemPacket(SystemOrder.CLIENT_CLOSE, reason))
        }

        extensionLoader.close()
        systemChannel.close(reason)
        socket.close(reason)
        tasksHandler.close(reason)
        trafficHandler.close(reason)

        open = false
        notifier.onClosed(relayId, reason)
        println("closed !")
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

    private def listen(reader: PacketReader): Unit = {
        try {
            val bytes = reader.readNextPacketBytes()
            //println(s"received : ${new String(bytes)}")
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
                if (serverErrConsole != null)
                    serverErrConsole.reportExceptionSimplified(e)
                close(CloseReason.INTERNAL_ERROR)

            case NonFatal(e) =>
                e.printStackTrace()

                Console.err.println(s"Suddenly disconnected from the server")
                if (serverErrConsole != null)
                    serverErrConsole.reportExceptionSimplified(e)
                close(CloseReason.INTERNAL_ERROR)
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
        case _: Packet => trafficHandler.injectPacket(packet, coordinates)
    }


    private def handleSystemPacket(system: SystemPacket, coords: PacketCoordinates): Unit = {
        val order = system.order
        val reason = system.reason.reversedPOV()
        val origin = coords.senderID

        notifier.onSystemOrderReceived(order, reason)

        import SystemOrder._
        order match {
            case CLIENT_CLOSE => close(origin, reason)
            case ABORT_TASK => tasksHandler.skipCurrent(reason)
            case PRINT_INFO => getConsoleOut(origin).orNull.println(s"$relayVersion (${Relay.ApiVersion})")

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

        if (!isConnected(targetID))
            throw new RelayException(s"Target '$targetID' does not exists !")
    }

    //initial tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close(CloseReason.INTERNAL)))
}