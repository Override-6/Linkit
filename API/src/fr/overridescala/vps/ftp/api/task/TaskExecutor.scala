package fr.overridescala.vps.ftp.api.task

import fr.overridescala.vps.ftp.api.Reason
import fr.overridescala.vps.ftp.api.packet.fundamental.TaskInitPacket
import fr.overridescala.vps.ftp.api.`extension`.packet.{PacketFactory, PacketManager}
import fr.overridescala.vps.ftp.api.packet.{Packet, PacketChannel, SyncPacketChannel}

/**
 * The class that will execute the Task.
 * When the task is ready to be executed, the method [[execute()]] will be called.
 * If the task was initialised by the local Relay, the getter [[initInfo]] will be used first.
 * The used channels kind are forced to be [[SyncPacketChannel]] because the Tasks are meant to be used concurrently
 *
 * @see [[Task]]
 * @see [[TaskAction]]
 * */
abstract class TaskExecutor {

    private var packetManager: PacketManager = _
    private var canCloseChannel: Boolean = true
    implicit protected var channel: PacketChannel.Sync = _


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

    final def init(packetManager: PacketManager, packetChannel: SyncPacketChannel): Unit = {
        if (packetManager == null || packetChannel == null)
            throw new NullPointerException
        this.packetManager = packetManager
        this.channel = packetChannel
    }

    def closeChannel(reason: Reason): Unit = {
        if (canCloseChannel)
            channel.close(reason)
    }

    protected def setDoNotCloseChannel(): Unit =
        canCloseChannel = false

    protected def setCloseChannel(): Unit =
        canCloseChannel = true

    final protected def registerPacketFactory[P <: Packet](packetClass: Class[P], factory: PacketFactory[P]): Unit = {
        packetManager.registerIfAbsent(packetClass, factory)
    }


}
