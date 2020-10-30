package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.packet.ext.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.packet.ext.{PacketFactory, PacketManager}
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel}

/**
 * The usable side for a [[TasksHandler]] to handle this task.
 * this trait can only execute (or pre-execute) tasks
 *
 * @see [[Task]]
 * @see [[TaskAction]]
 * */
abstract class TaskExecutor {

    private var packetManager: PacketManager = _
    private var canCloseChannel: Boolean = true
    implicit protected var channel: PacketChannel = _


    /**
     * This method value is used straight before task [[execute]] and only if a task where enqueued by the local Relay
     * It determines the way to instantiate a completer for this executor.
     *
     * @return a [[TaskInitInfo]] that describe how the completer will be instantiated
     * @see [[TaskInitPacket]]
     * */
    def initInfo: TaskInitInfo = null

    /**
     * Executes this task.
     * */
    def execute(): Unit

    final def init(packetManager: PacketManager, packetChannel: PacketChannel): Unit = {
        if (packetManager == null || packetChannel == null)
            throw new NullPointerException
        this.packetManager = packetManager
        this.channel = packetChannel
    }

    def closeChannel(): Unit = {
        if (canCloseChannel)
            channel.close()
    }

    protected def setDoNotCloseChannel(): Unit =
        canCloseChannel = false

    protected def setCloseChannel(): Unit =
        canCloseChannel = true

    final protected def registerPacketFactory[P <: Packet](packetClass: Class[P], factory: PacketFactory[P]): Unit = {
        packetManager.registerIfAbsent(packetClass, factory)
    }


}
