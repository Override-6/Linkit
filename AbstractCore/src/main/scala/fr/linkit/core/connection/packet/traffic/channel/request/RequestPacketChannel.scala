package fr.linkit.core.connection.packet.traffic.channel.request


import fr.linkit.api.connection.packet.channel.ChannelScope.ScopeFactory
import fr.linkit.api.connection.packet.channel.{ChannelScope, PacketChannel}
import fr.linkit.api.connection.packet.traffic.PacketInjectableFactory
import fr.linkit.api.connection.packet.traffic.injection.PacketInjection
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.traffic.ChannelScopes
import fr.linkit.core.connection.packet.traffic.channel.AbstractPacketChannel
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId
import fr.linkit.core.local.utils.ConsumerContainer
import org.jetbrains.annotations.Nullable

import java.util.NoSuchElementException
import scala.collection.mutable

class RequestPacketChannel(@Nullable parent: PacketChannel, scope: ChannelScope) extends AbstractPacketChannel(parent, scope) {

    private val requestHolders           = mutable.LinkedHashMap.empty[Long, RequestHolder]
    private val requestConsumers         = ConsumerContainer[RequestBundle]()
    @volatile private var requestID      = 0

    //debug only
    private val source = scope.traffic.supportIdentifier

    AppLogger.vDebug(s"Created $this, of parent $parent")

    override def handleInjection(injection: PacketInjection): Unit = {
        val coords = injection.coordinates
        injection.attachPin { (packet, attr) =>
            packet match {
                case request: RequestPacket =>
                    AppLogger.vDebug(this)
                    AppLogger.vDebug(s"${currentTasksId} <> $source: INJECTING REQUEST $request with attributes ${request.getAttributes}" + this)
                    request.setAttributes(attr)

                    val submitterScope = scope.shareWriter(ChannelScopes.retains(coords.senderID))
                    val submitter = new ResponseSubmitter(request.id, submitterScope)

                    requestConsumers.applyAllLater(RequestBundle(this, request, coords, submitter))

                case response: ResponsePacket =>
                    AppLogger.vDebug(s"${currentTasksId} <> $source: INJECTING RESPONSE $response with attributes ${response.getAttributes}" + this)
                    response.setAttributes(attr)

                    requestHolders.get(response.id) match {
                        case Some(request) => request.pushResponse(response)
                        case None          => throw new NoSuchElementException(s"(${Thread.currentThread().getName}) Response.id not found (${response.id}) ($requestHolders)")
                    }
            }
        }
    }

    def addRequestListener(callback: RequestBundle => Unit): Unit = {
        requestConsumers += callback
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

    override def createNew(@Nullable parent: PacketChannel, scope: ChannelScope): RequestPacketChannel = new RequestPacketChannel(parent, scope)
}
