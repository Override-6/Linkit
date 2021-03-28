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

package fr.linkit.api.connection.task

import fr.linkit.api.connection.ConnectionContext
import fr.linkit.api.connection.packet.traffic.{PacketSender, PacketSyncReceiver}

/**
 * The class that will execute the Task.
 * When the task is ready to be executed, the method [[execute()]] will be called.
 * If the task was initialised by the current connection, the getter [[initInfo]] will be used first.
 * The used channels kind are forced to be [[SyncPacketChannel]] because the Tasks are meant to be used concurrently
 *
 * @see [[Task]]
 * @see [[TaskAction]]
 * */
trait TaskExecutor {

    //def initInfo: TaskInitInfo = null

    /**
     * Executes this task.
     * */
    def execute(): Unit

    def init(connection: ConnectionContext, packetChannel: PacketSyncReceiver with PacketSender): Unit

}
