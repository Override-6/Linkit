package fr.linkit.core.connection.packet.traffic.channel.request

import fr.linkit.api.connection.packet.DedicatedPacketCoordinates
import fr.linkit.api.connection.packet.traffic.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectableFactory, PacketInjection}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.traffic.channel.AbstractPacketChannel
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId
import fr.linkit.core.local.utils.ConsumerContainer

import java.util.NoSuchElementException
import scala.collection.mutable

class RequestPacketChannel(scope: ChannelScope) extends AbstractPacketChannel(scope) {

    private val requestHolders      = mutable.LinkedHashMap.empty[Long, RequestHolder]
    private val requestConsumers    = ConsumerContainer[(SubmitterPacket, DedicatedPacketCoordinates, ResponseSubmitter)]()
    @volatile private var requestID = 0

    //debug only
    private val source = scope.traffic.supportIdentifier

    override def handleInjection(injection: PacketInjection): Unit = {
        val coords = injection.coordinates
        injection.attachPin(packet => {
            AppLogger.debug(s"${currentTasksId} <> $source: INJECTING REQUEST $packet " + this)
            packet match {
                case request: RequestPacket =>
                    val submitter = new ResponseSubmitter(request.id, scope)
                    requestConsumers.applyAllLater((request, coords, submitter))

                case response: ResponsePacket =>
                    requestHolders.get(response.id) match {
                        case Some(request) => request.pushResponse(response)
                        case None          => throw new NoSuchElementException(s"(${Thread.currentThread().getName}) Response.id not found (${response.id}) ($requestHolders)")
                    }
            }
        })
    }

    AppLogger.fatal(s"${currentTasksId} <> $source:  CREATED INSTANCE OF REQUEST PACKET CHANNEL : " + this)

    def addRequestListener(callback: (SubmitterPacket, DedicatedPacketCoordinates, ResponseSubmitter) => Unit): Unit = {
        AppLogger.fatal(s"$currentTasksId <> $source: ADDED REQUEST LISTENER : $requestHolders " + this)
        requestConsumers += (tuple => callback(tuple._1, tuple._2, tuple._3))
    }

    def makeRequest(scope: ScopeFactory[_ <: ChannelScope]): RequestSubmitter = {
        val pool = BusyWorkerPool.ensureCurrentIsWorker()

        val requestID = nextRequestID
        val writer    = traffic.newWriter(identifier)
        new RequestSubmitter(requestID, scope(writer), pool, this)
    }

    private def nextRequestID: Int = {
        requestID += 1
        requestID
    }

    private[request] def addRequestHolder(holder: RequestHolder): Unit = {
        requestHolders.put(holder.id, holder)
    }

    private[request] def removeRequestHolder(holder: RequestHolder): Unit = {
        requestHolders -= holder.id
    }

}

object RequestPacketChannel extends PacketInjectableFactory[RequestPacketChannel] {

    override def createNew(scope: ChannelScope): RequestPacketChannel = new RequestPacketChannel(scope)
}
