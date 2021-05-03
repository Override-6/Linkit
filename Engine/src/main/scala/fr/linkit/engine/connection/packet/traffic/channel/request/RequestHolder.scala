/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.connection.packet.traffic.channel.request

import fr.linkit.api.local.system.AppLogger
import fr.linkit.api.local.concurrency.WorkerPools.currentTasksId
import fr.linkit.engine.local.utils.ConsumerContainer

import java.util.concurrent.BlockingQueue

case class RequestHolder(id: Int, queue: BlockingQueue[SubmitterPacket], handler: RequestPacketChannel) {

    private val responseConsumer = ConsumerContainer[SubmitterPacket]()

    def nextResponse: SubmitterPacket = {
        AppLogger.vDebug(s"$currentTasksId <> Waiting for response... ($id) " + this)
        val response = queue.take()
        AppLogger.vError(s"$currentTasksId <> RESPONSE ($id) RECEIVED ! $response, $queue")
        response
    }

    def addOnResponseReceived(callback: SubmitterPacket => Unit): Unit = {
        responseConsumer += callback
    }

    def detach(): Unit = handler.removeRequestHolder(this)

    private[request] def pushResponse(response: SubmitterPacket): Unit = {
        AppLogger.vError(s"$currentTasksId <> ADDING RESPONSE $response FOR REQUEST $this")
        queue.add(response)
        responseConsumer.applyAllLater(response)
        AppLogger.vError(s"$currentTasksId <> RESPONSE $response ADDED TO REQUEST $this")
    }

}