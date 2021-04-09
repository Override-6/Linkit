package fr.linkit.core.connection.packet.traffic.channel.request

import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId
import fr.linkit.core.local.utils.ConsumerContainer

import java.util.concurrent.BlockingQueue

case class RequestHolder(id: Long, queue: BlockingQueue[SubmitterPacket], handler: RequestPacketChannel) {

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