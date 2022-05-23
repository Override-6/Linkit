/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic.channel.request

import fr.linkit.api.gnom.packet.channel.request.{ResponseHolder, SubmitterPacket}
import fr.linkit.api.internal.concurrency.WorkerPools
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.internal.utils.ConsumerContainer

import java.util.concurrent.BlockingQueue

case class SimpleResponseHolder(override val id: Int,
                                queue: BlockingQueue[AbstractSubmitterPacket],
                                handler: SimpleRequestPacketChannel) extends ResponseHolder {

    private val responseConsumer = ConsumerContainer[AbstractSubmitterPacket]()

    private var responseReceivedCount = 0
    private var responseSetCount = 0

    override def nextResponse: SubmitterPacket = {
        AppLogger.trace("next response on taskID: " + WorkerPools.currentTask.get.taskID)
        val response = queue.take()
        AppLogger.trace("got response on taskID: " + WorkerPools.currentTask.get.taskID)
        if (response == null)
            throw new NullPointerException("queue returned null response")
        this.synchronized {
            responseReceivedCount += 1
        }
        response
    }

    override def addOnResponseReceived(callback: SubmitterPacket => Unit): Unit = {
        responseConsumer += callback
    }

    private[request] def pushResponse(response: AbstractSubmitterPacket): Unit = {
        if (response == null)
            throw new NullPointerException()
        this.synchronized {
            responseSetCount += 1
        }

        queue.add(response)
        responseConsumer.applyAll(response)
    }

}