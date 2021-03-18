package fr.`override`.linkit.api.connection.task

import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.connection.packet.fundamental.TaskInitPacket
import fr.`override`.linkit.api.connection.packet.traffic.channel.SyncPacketChannel
import fr.`override`.linkit.api.connection.packet.traffic.{PacketSender, PacketSyncReceiver}
import fr.`override`.linkit.api.local.system.CloseReason

/**
 * The class that will execute the Task.
 * When the task is ready to be executed, the method [[execute()]] will be called.
 * If the task was initialised by the local Relay, the getter [[initInfo]] will be used first.
 * The used channels kind are forced to be [[SyncPacketChannel]] because the Tasks are meant to be used concurrently
 *
 * @see [[Task]]
 * @see [[TaskAction]]
 * */
trait TaskExecutor {

    /**
     * This method value is used straight before task [[execute]] and only if a task where enqueued by the local Relay
     * It determines the way to instantiate a completer for this executor.
     *
     * @return a [[TaskInitInfo]] that describe how the completer will be instantiated
     * */
    def initInfo: TaskInitInfo = null

    /**
     * Executes this task.
     * */
    def execute(): Unit

    protected def setDoNotCloseChannel(): Unit

    protected def setCloseChannel(): Unit

}
