package fr.`override`.linkit.api.task

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.exception.TaskException
import fr.`override`.linkit.api.packet.channel.{PacketChannel, SyncPacketChannel}
import fr.`override`.linkit.api.packet.fundamental.TaskInitPacket
import fr.`override`.linkit.api.system.{Reason, RemoteConsole}

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

    private var canCloseChannel: Boolean = true
    implicit protected var relay: Relay = _
    implicit protected var channel: PacketChannel.Sync = _
    protected var remoteConsoleErr: RemoteConsole.Err = _
    protected var remoteConsoleOut: RemoteConsole = _


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

    final def init(relay: Relay, targetID: String, packetChannel: PacketChannel.Sync): Unit = {
        if (relay == null || packetChannel == null)
            throw new NullPointerException
        this.channel = packetChannel

        this.remoteConsoleErr = relay.getConsoleErr(targetID).orNull
        this.remoteConsoleOut = relay.getConsoleOut(targetID).orNull
        if (remoteConsoleOut == null || remoteConsoleErr == null)
            throw new TaskException(s"Could not initialise task : could not retrieve remote console of relay $targetID")

        this.relay = relay
    }

    def closeChannel(reason: Reason): Unit = {
        if (canCloseChannel)
            channel.close(reason)
    }

    protected def setDoNotCloseChannel(): Unit =
        canCloseChannel = false

    protected def setCloseChannel(): Unit =
        canCloseChannel = true

}
