package fr.`override`.linkit.client

import java.nio.channels.AsynchronousCloseException
import java.nio.charset.Charset

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.`extension`.{RelayExtensionLoader, RelayProperties}
import fr.`override`.linkit.api.exception._
import fr.`override`.linkit.api.network._
import fr.`override`.linkit.api.packet.channel.{PacketChannel, PacketChannelFactory}
import fr.`override`.linkit.api.packet.collector.{PacketCollector, PacketCollectorFactory}
import fr.`override`.linkit.api.packet.fundamental._
import fr.`override`.linkit.api.packet.traffic.{DedicatedPacketTraffic, PacketReader}
import fr.`override`.linkit.api.packet.{PacketTranslator, _}
import fr.`override`.linkit.api.system._
import fr.`override`.linkit.api.system.security.RelaySecurityManager
import fr.`override`.linkit.api.task.{Task, TaskCompleterHandler}
import fr.`override`.linkit.client.config.RelayPointConfiguration
import fr.`override`.linkit.client.network.PointNetwork

import scala.reflect.ClassTag
import scala.util.control.NonFatal

object RelayPoint {
    val version: Version = Version(name = "RelayPoint", version = "0.11.0", stable = false)
}

class RelayPoint private[client](override val configuration: RelayPointConfiguration) extends Relay {

    @volatile private var open = false
    override val packetTranslator = new PacketTranslator(this)
    private val socket                    : ClientDynamicSocket     = new ClientDynamicSocket(configuration.serverAddress, configuration.reconnectionPeriod)
    override val traffic                  : DedicatedPacketTraffic  = new DedicatedPacketTraffic(this, socket, identifier)
    override val extensionLoader          : RelayExtensionLoader    = new RelayExtensionLoader(this)
    override val properties               : RelayProperties         = new RelayProperties
    implicit val systemChannel            : SystemPacketChannel     = new SystemPacketChannel(Relay.ServerIdentifier, traffic)
    private val tasksHandler              : ClientTasksHandler      = new ClientTasksHandler(systemChannel, this)
    private val remoteConsoles            : RemoteConsolesContainer = new RemoteConsolesContainer(this)
    private lazy val serverErrConsole     : RemoteConsole           = getConsoleErr(Relay.ServerIdentifier)
    override val securityManager          : RelaySecurityManager    = configuration.securityManager
    override val taskCompleterHandler     : TaskCompleterHandler    = tasksHandler.tasksCompleterHandler
    override val relayVersion             : Version                 = RelayPoint.version
    private var pointNetwork              : PointNetwork            = _ //will be instantiated once connected
    override def network                  : Network                 = pointNetwork

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
    }

    override def close(reason: CloseReason): Unit = {
        if (!open)
            return //already closed.
        println("Closing...")

        if (reason.isInternal && isConnected) {
            systemChannel.sendPacket(SystemPacket(SystemOrder.CLIENT_CLOSE, reason))
        }

        extensionLoader.close()
        systemChannel.close(reason)
        socket.close(reason)
        tasksHandler.close(reason)
        traffic.close(reason)

        open = false
        println("closed !")
    }

    def isConnected: Boolean = socket.getState == ConnectionState.CONNECTED

    override def isConnected(identifier: String): Boolean = {
        systemChannel.sendOrder(SystemOrder.CHECK_ID, CloseReason.INTERNAL, identifier.getBytes)
        val response = systemChannel.nextPacket(DataPacket).header
        response == "OK"
    }

    private def loadRemote(): Unit = {
        println(s"Connecting to server with relay id '$identifier'")
        traffic.register(systemChannel)

        val idLength = identifier.length
        val welcomePacket = Array(idLength.toByte) ++ identifier.getBytes
        socket.write(welcomePacket)
        socket.addConnectionStateListener(state => if (state == ConnectionState.CONNECTED) socket.write(welcomePacket))

        val response = systemChannel.nextPacket(DataPacket)
        if (response.header == "ERROR")
            throw RelayInitialisationException(new String(response.content))

        systemChannel.sendOrder(SystemOrder.PRINT_INFO, CloseReason.INTERNAL)
        println("Connected !")

        println("Initialising Network...")
        this.pointNetwork = new PointNetwork(this)
        println("Network initialised !")
    }

    override def getState: ConnectionState = socket.getState

    override def isClosed: Boolean = !open

    override def openChannel[C <: PacketChannel : ClassTag](channelId: Int, targetID: String, factory: PacketChannelFactory[C]): C = {
        traffic.openChannel(channelId, targetID, factory)
    }

    override def openCollector[C <: PacketCollector : ClassTag](channelId: Int, factory: PacketCollectorFactory[C]): C = {
        traffic.openCollector(channelId, factory)

    }

    override def getConsoleOut(targetId: String): RemoteConsole = remoteConsoles.getOut(targetId)

    override def getConsoleErr(targetId: String): RemoteConsole = remoteConsoles.getErr(targetId)


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

    override def addConnectionListener(action: ConnectionState => Unit): Unit = socket.addConnectionStateListener(action)

    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        ensureTargetValid(task.targetID)
        if (!configuration.enableTasks)
            throw new TaskException("Task handling is disabled according to RelayConfiguration")

        task.preInit(tasksHandler)
        RelayTaskAction.of(task)
    }


    private def loadLocal(): Unit = {
        if (configuration.enableTasks) {
            println("Loading tasks handler...")
            tasksHandler.start()
        }
        if (configuration.enableExtensionsFolderLoad) {
            println("Loading Relay extensions from folder " + configuration.extensionsFolder)
            extensionLoader.launch()
        }
    }

    private def listen(reader: PacketReader): Unit = {
        try {
            val bytes = reader.readNextPacketBytes()
            //NETWORK-DEBUG-MARK
            println(s"received : ${new String(bytes)}")
            if (bytes == null)
                return
            val (packet, coordinates) = packetTranslator.toPacketAndCoords(bytes)

            if (configuration.checkReceivedPacketTargetID)
                checkPacket(coordinates)

            handlePacket(packet, coordinates)
        }
        catch {
            case e: AsynchronousCloseException =>
                Console.err.println("Asynchronous close.")
                if (serverErrConsole != null)
                    serverErrConsole.print(e)
                close(CloseReason.INTERNAL_ERROR)

            case NonFatal(e) =>
                e.printStackTrace(System.out)

                Console.err.println(s"Suddenly disconnected from the server")
                if (serverErrConsole != null)
                    serverErrConsole.print(e)
                close(CloseReason.INTERNAL_ERROR)
        }
    }

    private def checkPacket(coordinates: PacketCoordinates): Unit = {
        val targetID = coordinates.targetID
        if (targetID == identifier || targetID == "unknown")
            return

        val sender = coordinates.senderID
        val consoleOut = getConsoleErr(sender)
        val msg = s"Could not handle packet : targetID ($targetID) isn't equals to this relay identifier !"
        consoleOut.println(msg)
        throw new UnexpectedPacketException(msg)
    }

    private def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = packet match {
        case init: TaskInitPacket => tasksHandler.handlePacket(init, coordinates)
        case system: SystemPacket => handleSystemPacket(system, coordinates)
        case _: Packet => traffic.injectPacket(packet, coordinates)
    }


    private def handleSystemPacket(system: SystemPacket, coords: PacketCoordinates): Unit = {
        val order = system.order
        val reason = system.reason.reversedPOV()
        val origin = coords.senderID

        import SystemOrder._
        order match {
            case CLIENT_CLOSE => close(reason)
            case ABORT_TASK => tasksHandler.skipCurrent(reason)
            case PRINT_INFO => getConsoleOut(origin).println(s"$relayVersion (${Relay.ApiVersion})")

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

}