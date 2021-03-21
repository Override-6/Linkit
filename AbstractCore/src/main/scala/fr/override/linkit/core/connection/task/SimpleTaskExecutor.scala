/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.core.connection.task

import fr.`override`.linkit.api.connection.ConnectionContext
import fr.`override`.linkit.api.connection.packet.traffic.{PacketReceiver, PacketSender}
import fr.`override`.linkit.api.connection.task.TaskExecutor
import fr.`override`.linkit.api.local.system.Reason
import fr.`override`.linkit.core.connection.packet.traffic.channel

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
    protected var connection: ConnectionContext = _
    protected var channel: PacketReceiver with PacketSender = _


    final def init(connection: ConnectionContext, packetChannel: PacketReceiver with PacketSender): Unit = {
        if (connection == null || packetChannel == null)
            throw new NullPointerException

        this.channel = packetChannel
        this.connection = connection
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
