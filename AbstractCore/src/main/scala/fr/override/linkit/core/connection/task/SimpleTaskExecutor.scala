package fr.`override`.linkit.core.connection.task

import fr.`override`.linkit.core.connection.packet.traffic
import fr.`override`.linkit.core.connection.packet.traffic.channel
import fr.`override`.linkit.api.Relay
import fr.`override`.linkit.api.connection.packet.traffic.{PacketSender, PacketReceiver}
import fr.`override`.linkit.api.connection.task.TaskExecutor
import fr.`override`.linkit.api.local.system.CloseReason

/**
 * The class that will execute the Task.
 * When the task is ready to be executed, the method [[execute()]] will be called.
 * If the task was initialised by the local Relay, the getter [[initInfo]] will be used first.
 * The used channels kind are forced to be [[channel.SyncPacketChannel]] because the Tasks are meant to be used concurrently
 *
 * @see [[SimpleTask]]
 * */
abstract class SimpleTaskExecutor extends TaskExecutor {

    private var canCloseChannel: Boolean = true
    implicit protected var relay: Relay = _
    implicit protected var channel: PacketReceiver with PacketSender = _


    final def init(relay: Relay, packetChannel: traffic.channel.SyncPacketChannel): Unit = {
        if (relay == null || packetChannel == null)
            throw new NullPointerException
        this.channel = packetChannel

        this.relay = relay
    }

    def closeChannel(reason: CloseReason): Unit = {
        if (canCloseChannel)
            channel.close(reason)
    }

    protected def setDoNotCloseChannel(): Unit =
        canCloseChannel = false

    protected def setCloseChannel(): Unit =
        canCloseChannel = true

}
