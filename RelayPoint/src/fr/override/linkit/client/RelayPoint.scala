package fr.`override`.linkit.client

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.Relay.ServerIdentifier
import fr.`override`.linkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.`override`.linkit.api.concurrency.{PacketWorkerThread, RelayWorkerThreadPool}
import fr.`override`.linkit.api.exception._
import fr.`override`.linkit.api.network._
import fr.`override`.linkit.api.packet._
import fr.`override`.linkit.api.packet.fundamental.RefPacket.StringPacket
import fr.`override`.linkit.api.packet.fundamental.ValPacket.BooleanPacket
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.packet.serialization.PacketTranslator
import fr.`override`.linkit.api.packet.traffic.ChannelScope.ScopeFactory
import fr.`override`.linkit.api.packet.traffic.PacketTraffic.SystemChannelID
import fr.`override`.linkit.api.packet.traffic._
import fr.`override`.linkit.api.system.RelayState._
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.system.event.EventNotifier
import fr.`override`.linkit.api.system.event.relay.RelayEvents
import fr.`override`.linkit.api.system.security.RelaySecurityManager
import fr.`override`.linkit.api.task.{Task, TaskCompleterHandler}
import fr.`override`.linkit.client.config.RelayPointConfiguration
import fr.`override`.linkit.client.network.PointNetwork

import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset
import scala.reflect.ClassTag
import scala.util.control.NonFatal

object RelayPoint {
    val version: Version = Version(name = "RelayPoint", version = "0.14.0", stable = false)
}

class RelayPoint private[client](override val configuration: RelayPointConfiguration) extends Relay {

    @volatile private var open = false

    private var currentState: RelayState = RelayState.INACTIVE
    override val relayVersion: Version = RelayPoint.version
    private var pointNetwork: PointNetwork = _ //will be instantiated once connected
    override def network: Network = pointNetwork

    override val securityManager: RelaySecurityManager = configuration.securityManager
    override val notifier: EventNotifier = new EventNotifier
    private val socket: ClientDynamicSocket = new ClientDynamicSocket(configuration.serverAddress, configuration.reconnectionPeriod)
    override val packetTranslator: PacketTranslator = new PacketTranslator(this)
    override val traffic: SocketPacketTraffic = new SocketPacketTraffic(this, socket, identifier)
    override val extensionLoader: RelayExtensionLoader = new RelayExtensionLoader(this)
    override val properties: RelayProperties = new RelayProperties()
    private val workerThread: RelayWorkerThreadPool = new RelayWorkerThreadPool("Packet Handling & Extension", 3)
    implicit val systemChannel: SystemPacketChannel = traffic.createInjectable(SystemChannelID, ChannelScope.reserved(ServerIdentifier), SystemPacketChannel)
    private val tasksHandler: ClientTasksHandler = new ClientTasksHandler(systemChannel, this)
    private val remoteConsoles: RemoteConsolesContainer = new RemoteConsolesContainer(this)
    override val taskCompleterHandler: TaskCompleterHandler = new TaskCompleterHandler()

    override def start(): Unit = {
        RelayWorkerThreadPool.checkCurrentIsWorker("Must start relay point in a worker thread.")
        setState(ENABLING)

        val t0 = System.currentTimeMillis()
        open = true
        securityManager.checkRelay(this)

        println("Current encoding is " + Charset.defaultCharset().name())
        println("Listening on port " + configuration.serverAddress.getPort)
        println("Computer name is " + System.getenv().get("COMPUTERNAME"))
        println(relayVersion)
        println(Relay.ApiVersion)


        try {
            PointPacketWorkerThread.start()
            loadRemote()
            loadUserFeatures()
        } catch {
            case e: RelayInitialisationException =>
                setState(CRASHED)
                throw e

            case NonFatal(e) =>
                close(CloseReason.INTERNAL_ERROR) //state 'CRASHED' will be set into the close method.
                throw RelayInitialisationException(e.getMessage, e)
        }
        securityManager.checkRelay(this)

        val t1 = System.currentTimeMillis()
        println(s"Ready ! (took ${t1 - t0}ms)")
        setState(ENABLED)
    }

    override def runLater(callback: => Unit): Unit = {
        workerThread.runLater(callback)
    }

    override def state(): RelayState = currentState

    override def close(reason: CloseReason): Unit = {
        RelayWorkerThreadPool.checkCurrentIsWorker()

        if (!open)
            return //already closed

        if (reason.isInternal && isConnected) {
            systemChannel.send(SystemPacket(SystemOrder.CLIENT_CLOSE, reason))
        }

        //Closing workers
        PointPacketWorkerThread.close(reason)
        workerThread.close()

        //Closing Tasks and extensions
        extensionLoader.close()
        tasksHandler.close(reason)

        //Closing Traffic
        systemChannel.close(reason)
        traffic.close(reason)

        //Closing socket
        socket.close(reason)

        //Concluding close
        open = false
        if (reason == CloseReason.INTERNAL_ERROR) setState(CRASHED)
        else setState(CLOSED)

        println("closed !")
    }

    def isConnected: Boolean = socket.getState == ConnectionState.CONNECTED

    override def isConnected(identifier: String): Boolean = {
        systemChannel.sendOrder(SystemOrder.CHECK_ID, CloseReason.INTERNAL, identifier.getBytes)
        systemChannel.nextPacket[BooleanPacket]
    }

    override def getConnectionState: ConnectionState = socket.getState

    override def isClosed: Boolean = !open

    override def getInjectable[C <: PacketInjectable : ClassTag](channelId: Int, scopeFactory: ScopeFactory[_ <: ChannelScope], factory: PacketInjectableFactory[C]): C = {
        traffic.createInjectable(channelId, scopeFactory, factory)
    }

    override def getConsoleOut(targetId: String): RemoteConsole = remoteConsoles.getOut(targetId)

    override def getConsoleErr(targetId: String): RemoteConsole = remoteConsoles.getErr(targetId)

    override def addConnectionListener(action: ConnectionState => Unit): Unit = socket.addConnectionStateListener(action)

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        ensureTargetValid(task.targetID)
        if (!configuration.enableTasks)
            throw new TaskException("Task handling is disabled according to RelayConfiguration")

        task.preInit(tasksHandler)
        RelayTaskAction.of(task)
    }

    private def handleSystemPacket(system: SystemPacket, coords: DedicatedPacketCoordinates): Unit = {
        val order = system.order
        val reason = system.reason.reversedPOV()
        val sender = coords.senderID

        import SystemOrder._
        order match {
            case CLIENT_CLOSE => close(reason)
            case ABORT_TASK => tasksHandler.skipCurrent(reason)
            case PRINT_INFO => getConsoleOut(sender).println(s"$relayVersion (${Relay.ApiVersion})")

            //FIXME weird use of exceptions/remote print
            case _@(SERVER_CLOSE | CHECK_ID) =>
                new UnexpectedPacketException(s"System packet order '$order' couldn't be handled by this RelayPoint : Received forbidden order")
                        .printStackTrace(getConsoleErr(sender))

            case _ => new UnexpectedPacketException(s"System packet order '$order' couldn't be handled by this RelayPoint : Unknown order")
                    .printStackTrace(getConsoleErr(sender))
        }
    }

    private def loadRemote(): Unit = {
        println(s"Connecting to server with relay id '$identifier'")
        setState(CONNECTING)
        socket.start()
        println(s"Socket accepted...")

        val welcomePacket = PacketUtils.wrap(identifier.getBytes)
        socket.write(welcomePacket)
        socket.addConnectionStateListener(state => if (state == ConnectionState.CONNECTED) socket.write(welcomePacket))

        val accepted = systemChannel.nextPacket[BooleanPacket]
        if (!accepted) {
            val refusalMessage = systemChannel.nextPacket[StringPacket].value
            throw RelayInitialisationException(refusalMessage)
        }
        systemChannel.sendOrder(SystemOrder.PRINT_INFO, CloseReason.INTERNAL)
        println("Connection accepted !")
        setState(ENABLING)

        println("Initialising Network...")
        this.pointNetwork = new PointNetwork(this)
        println("Network initialised !")
    }

    private def loadUserFeatures(): Unit = {
        if (configuration.enableTasks) {
            println("Loading tasks handler...")
            tasksHandler.start()
        }
        if (configuration.enableExtensionsFolderLoad) {
            println("Loading Relay extensions from folder " + configuration.extensionsFolder)
            extensionLoader.launch()
        }
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new RelayCloseException("Relay Point have to be started !")
    }

    private def setState(state: RelayState): Unit = {
        notifier.notifyEvent(RelayEvents.stateChange(state))
        this.currentState = state
    }

    private def ensureTargetValid(targetID: String): Unit = {
        if (targetID == identifier)
            throw new RelayException("Can't start any task with oneself !")

        if (!isConnected(targetID))
            throw new RelayException(s"Target '$targetID' does not exists !")
    }

    private def checkCoordinates(coordinates: DedicatedPacketCoordinates): Unit = {
        val targetID = coordinates.targetID
        if (targetID == identifier || targetID == "unknown")
            return

        val sender = coordinates.senderID
        val consoleErr = getConsoleErr(sender)
        val msg = s"Could not handle packet : targetID ($targetID) isn't equals to this relay identifier !"
        consoleErr.println(msg)
        throw new UnexpectedPacketException(msg)
    }

    private def handlePacket(packet: Packet, coordinates: DedicatedPacketCoordinates, number: Int): Unit = {
        packet match {
            case init: TaskInitPacket => tasksHandler.handlePacket(init, coordinates)
            case system: SystemPacket => handleSystemPacket(system, coordinates)
            case _: Packet =>
                val injection = PacketInjections.createInjection(packet, coordinates, number)
                //println(s"START OF INJECTION ($packet, $coordinates, $number) - ${Thread.currentThread()}")
                traffic.handleInjection(injection)
            //println(s"ENT OF INJECTION ($packet, $coordinates, $number) - ${Thread.currentThread()}")
        }
    }

    object PointPacketWorkerThread extends PacketWorkerThread() {

        private val packetReader = new PacketReader(socket, securityManager)
        @volatile private var packetsReceived = 0

        override protected def refresh(): Unit = {
            try {
                listen()
            } catch {
                case _: AsynchronousCloseException =>
                    Console.err.println("Asynchronous close.")

                    runLater {
                        RelayPoint.this.close(CloseReason.INTERNAL_ERROR)
                    }

                case NonFatal(e) =>
                    e.printStackTrace(System.out)
                    Console.err.println(s"Suddenly disconnected from the server.")

                    runLater {
                        RelayPoint.this.close(CloseReason.INTERNAL_ERROR)
                    }
            }
        }

        private def listen(): Unit = {
            val bytes = packetReader.readNextPacketBytes()
            if (bytes == null)
                return
            //NETWORK-DEBUG-MARK
            //println(s"received : ${new String(bytes.take(1000)).replace('\n', ' ').replace('\r', ' ')} (l: ${bytes.length})")
            val packetNumber = packetsReceived + 1
            packetsReceived += 1

            runLater { //handles and deserializes the packet in the worker thread pool
                val (packet, coordinates) = packetTranslator.toPacketAndCoords(bytes)

                //println(s"RECEIVED PACKET $packet WITH COORDINATES $coordinates. This packet will be handled in thread ${Thread.currentThread()}")

                coordinates match {
                    case dedicated: DedicatedPacketCoordinates =>
                        if (configuration.checkReceivedPacketTargetID)
                            checkCoordinates(dedicated)
                        handlePacket(packet, dedicated, packetNumber)
                    case other => throw new UnexpectedPacketException(s"Only DedicatedPacketCoordinates can be handled by a RelayPoint. Received : ${other.getClass.getName}")
                }
            }
        }

    }

}