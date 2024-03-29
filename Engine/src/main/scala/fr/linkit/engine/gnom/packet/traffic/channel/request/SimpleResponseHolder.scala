/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.gnom.packet.traffic.channel.request

import fr.linkit.api.gnom.packet.channel.request.{ResponseHolder, SubmitterPacket}
import fr.linkit.engine.internal.concurrency.RequestReleasedReentrantLock
import fr.linkit.engine.internal.util.ConsumerContainer

import java.util.concurrent.BlockingQueue

case class SimpleResponseHolder(override val id: Int,
                                queue          : BlockingQueue[AbstractSubmitterPacket],
                                handler        : SimpleRequestPacketChannel) extends ResponseHolder {

    private val responseConsumer = ConsumerContainer[AbstractSubmitterPacket]()

    override def nextResponse: SubmitterPacket = RequestReleasedReentrantLock.runReleased {
        val response = queue.take()
        if (response == null)
            throw new NullPointerException("queue returned null response")
        response
    }

    override def addOnResponseReceived(callback: SubmitterPacket => Unit): Unit = {
        responseConsumer += callback
    }

    private[request] def pushResponse(response: AbstractSubmitterPacket): Unit = {
        if (response == null)
            throw new NullPointerException()

        queue.add(response)
        responseConsumer.applyAll(response)
    }

}