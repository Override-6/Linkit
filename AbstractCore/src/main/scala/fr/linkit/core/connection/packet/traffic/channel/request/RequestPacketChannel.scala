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
import scala.collection.mutable.ListBuffer

class RequestPacketChannel(scope: ChannelScope) extends AbstractPacketChannel(scope) {

    private val requestHolders           = mutable.LinkedHashMap.empty[Long, RequestHolder]
    private val requestConsumers         = ConsumerContainer[(RequestPacket, DedicatedPacketCoordinates, ResponseSubmitter)]()
    private val storedResponseSubmitters = ListBuffer.empty[(RequestPacket, DedicatedPacketCoordinates, ResponseSubmitter)]
    @volatile private var requestID      = 0

    //debug only
    private val source = scope.traffic.supportIdentifier

    override def handleInjection(injection: PacketInjection): Unit = {
        val coords = injection.coordinates
        injection.attachPin { (packet, attr) =>
            packet match {
                case request: RequestPacket =>
                    request.setAttributes(attr)

                    AppLogger.debug(s"${currentTasksId} <> $source: INJECTING REQUEST $request " + this)
                    val submitter = new ResponseSubmitter(request.id, scope)
                    requestConsumers.applyAllLater((request, coords, submitter))

                case response: ResponsePacket =>
                    AppLogger.debug(s"${currentTasksId} <> $source: INJECTING RESPONSE $response " + this)
                    response.setAttributes(attr)

                    requestHolders.get(response.id) match {
                        case Some(request) => request.pushResponse(response)
                        case None          => throw new NoSuchElementException(s"(${Thread.currentThread().getName}) Response.id not found (${response.id}) ($requestHolders)")
                    }
            }
        }
    }

    def addRequestListener(callback: (RequestPacket, DedicatedPacketCoordinates, ResponseSubmitter) => Unit): Unit = {
        AppLogger.fatal(s"$currentTasksId <> $source: ADDED REQUEST LISTENER : $requestHolders " + this)
        requestConsumers += (tuple => callback(tuple._1, tuple._2, tuple._3))
    }

    def makeRequest(scopeFactory: ScopeFactory[_ <: ChannelScope]): RequestSubmitter = {
        val writer = traffic.newWriter(identifier)
        makeRequest(scopeFactory(writer))
    }

    def makeRequest(scope: ChannelScope): RequestSubmitter = {
        val pool = BusyWorkerPool.ensureCurrentIsWorker()

        val requestID = nextRequestID
        new RequestSubmitter(requestID, scope, pool, this)
    }

    def storeSubmitter(packet: RequestPacket, coords: DedicatedPacketCoordinates, submitter: ResponseSubmitter): Unit = {
        storedResponseSubmitters += ((packet, coords, submitter))
    }

    def injectStoredSubmitters(): Unit = {
        storedResponseSubmitters.foreach(triplet => {
            requestConsumers.applyAllLater((triplet._1, triplet._2, triplet._3))
        })
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
