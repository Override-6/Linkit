package fr.overridescala.vps.ftp.server

import java.net.{ServerSocket, SocketException}
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}

import fr.overridescala.vps.ftp.api.{Relay, RelayProperties}
import fr.overridescala.vps.ftp.api.exceptions.RelayException
import fr.overridescala.vps.ftp.api.packet.{PacketChannel, PacketChannelManagerCache, SimplePacketChannel}
import fr.overridescala.vps.ftp.api.packet.ext.PacketManager
import fr.overridescala.vps.ftp.api.task.ext.TaskLoader
import fr.overridescala.vps.ftp.api.task.{Task, TaskCompleterHandler}
import fr.overridescala.vps.ftp.api.utils.Constants
import fr.overridescala.vps.ftp.server.connection.ConnectionsManager

import scala.util.control.NonFatal

class RelayServer extends Relay {

    Map.empty

    private val serverSocket = new ServerSocket(Constants.PORT)
    private val connectionsManager = new ConnectionsManager(this)
    //Awful thing, only for debugging, and easily switch from localhost to vps.
    private val taskFolderPath =
        if (System.getenv().get("COMPUTERNAME") == "PC_MATERIEL_NET") Paths.get("C:\\Users\\maxim\\Desktop\\Dev\\VPS\\ClientSide\\Tasks")
        else Paths.get("Tasks/")
    @volatile private var open = false

    /**
     * For safety, prefer Relay#identfier instead of Constants.SERVER_ID
     * */
    override val identifier: String = Constants.SERVER_ID
    override val packetManager = new PacketManager
    override val taskLoader = new TaskLoader(this, taskFolderPath)
    override val taskCompleterHandler = new TaskCompleterHandler
    override val properties: RelayProperties = new RelayProperties


    override def scheduleTask[R](task: Task[R]): RelayTaskAction[R] = {
        ensureOpen()
        val targetIdentifier = task.targetID
        val connection = connectionsManager.getConnectionFromIdentifier(targetIdentifier)
        if (connection == null)
            throw new NoSuchElementException(s"Unknown or unregistered relay with identifier '$targetIdentifier'")

        val tasksHandler = connection.tasksHandler
        task.preInit(tasksHandler, identifier)
        RelayTaskAction(task)
    }


    override def start(): Unit = {
        println("Ready !")
        println("Current encoding is " + Charset.defaultCharset().name())
        println("Listening on port " + Constants.PORT)
        println("Computer name is " + System.getenv().get("COMPUTERNAME"))

        taskLoader.refreshTasks()

        open = true
        while (open) awaitClientConnection()
    }


    override def createChannel(linkedRelayID: String, id: Int): PacketChannel =
        createChannelAndManager(linkedRelayID, id)

    override def close(): Unit = {
        println("closing server...")
        connectionsManager.close()
        serverSocket.close()
        open = false
        println("server closed !")
    }

    def createChannelAndManager(linkedRelayID: String, id: Int): SimplePacketChannel = {
        val targetConnection = connectionsManager.getConnectionFromIdentifier(linkedRelayID)
        targetConnection.createChannel(id)
    }

    private def awaitClientConnection(): Unit = {
        try {
            val clientSocket = serverSocket.accept()
            connectionsManager.register(clientSocket)
        } catch {
            case e: RelayException => Console.err.println(e.getMessage)
            case e: SocketException if e.getMessage == "Socket closed" =>
                Console.err.println(e.getMessage)
                close()
            case NonFatal(e) => e.printStackTrace()
        }
    }

    private def ensureOpen(): Unit = {
        if (!open)
            throw new UnsupportedOperationException("Relay Point have to be started !")
    }

    // default tasks
    Runtime.getRuntime.addShutdownHook(new Thread(() => close()))

}