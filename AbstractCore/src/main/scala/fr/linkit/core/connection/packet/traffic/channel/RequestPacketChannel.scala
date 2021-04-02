package fr.linkit.core.connection.packet.traffic.channel

import fr.linkit.api.connection.packet.traffic.{ChannelScope, PacketInjectableFactory, PacketInjection}
import fr.linkit.api.connection.packet.{DedicatedPacketCoordinates, Packet}
import fr.linkit.api.local.system.AppLogger
import fr.linkit.core.connection.packet.fundamental.RefPacket.AnyRefPacket
import fr.linkit.core.connection.packet.traffic.channel.RequestPacketChannel.{Request, Response, ResponseSubmitter}
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool
import fr.linkit.core.local.concurrency.pool.BusyWorkerPool.currentTasksId
import fr.linkit.core.local.utils.ConsumerContainer
import fr.linkit.core.local.utils.ScalaUtils.ensureType

import java.util.NoSuchElementException
import java.util.concurrent.BlockingQueue
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

class RequestPacketChannel(scope: ChannelScope) extends AbstractPacketChannel(scope) {

    private[RequestPacketChannel] val requests         = mutable.LinkedHashMap.empty[Long, Request]
    private                       val requestConsumers = ConsumerContainer[(Packet, DedicatedPacketCoordinates, ResponseSubmitter)]()
    @volatile private var requestID                    = 0
    //debug only
    private                       val source           = scope.traffic.supportIdentifier

    override def handleInjection(injection: PacketInjection): Unit = {
        val coords = injection.coordinates
        injection.process(packet => {
            AppLogger.debug(s"${currentTasksId} <> $source: INJECTING REQUEST $packet " + this)
            packet match {
                case AnyRefPacket((id: Int, packet: Packet)) =>
                    val submitter = new ResponseSubmitter(id, scope, coords.senderID, source)
                    requestConsumers.applyAllLater((packet, coords, submitter))

                case AnyRefPacket((id: Long, packet: Packet)) =>
                    val submitter = new ResponseSubmitter(id, scope, coords.senderID, source)
                    requestConsumers.applyAllLater((packet, coords, submitter))

                case response: Response =>
                    requests.get(response.id) match {
                        case Some(request) => request.addResponse(response)
                        case None          => throw new NoSuchElementException(s"(${Thread.currentThread().getName}) Response.id not found (${response.id}) ($requests)")
                    }
            }
        })
    }

    AppLogger.fatal(s"${currentTasksId} <> $source:  CREATED INSTANCE OF REQUEST PACKET CHANNEL : " + this)

    def addRequestListener(callback: (Packet, DedicatedPacketCoordinates, ResponseSubmitter) => Unit): Unit = {
        AppLogger.fatal(s"$currentTasksId <> $source: ADDED REQUEST LISTENER : $requests " + this)
        requestConsumers += (tuple => callback(tuple._1, tuple._2, tuple._3))
    }

    def startRequest(packet: Packet, targets: String*): Request = {
        sendRequest(packet, scope.sendTo(_, targets: _*))
    }

    def broadcastRequest(packet: Packet): Request = {
        sendRequest(packet, scope.sendToAll)
    }

    private def sendRequest(packet: Packet, send: Packet => Unit): Request = {
        val pool = BusyWorkerPool.ensureCurrentIsWorker()

        val requestID = nextRequestID
        val request   = Request(requestID, pool.newBusyQueue, this, source)

        AppLogger.error(s"$currentTasksId <> $source: Adding request '$request' into $requests " + this)
        requests.put(requestID, request)
        send(AnyRefPacket(requestID, packet))
        AppLogger.error(s"$currentTasksId <> $source: Request '${request}' has been sent ! " + this)
        request
    }

    private def nextRequestID: Int = {
        requestID += 1
        requestID
    }

}

object RequestPacketChannel extends PacketInjectableFactory[RequestPacketChannel] {

    override def createNew(scope: ChannelScope): RequestPacketChannel = new RequestPacketChannel(scope)

    case class Request(id: Long, queue: BlockingQueue[Response], handler: RequestPacketChannel, source: String) {

        private val responseConsumer = ConsumerContainer[Response]()

        def nextResponse: Response = {
            AppLogger.debug(s"$currentTasksId <> $source: Waiting for response... ($id) " + this)
            val response = queue.take()
            AppLogger.error(s"$currentTasksId <> $source: RESPONSE ($id) RECEIVED ! $response, $queue")
            response
        }

        def addOnResponseReceived(callback: Response => Unit): Unit = {
            responseConsumer += callback
        }

        def delete(): Unit = handler.requests.remove(id)

        private[RequestPacketChannel] def addResponse(response: Response): Unit = {
            AppLogger.error(s"$currentTasksId <> $source: ADDING RESPONSE $response FOR REQUEST $this")
            queue.add(response)
            responseConsumer.applyAllLater(response)
            AppLogger.error(s"$currentTasksId <> $source: RESPONSE $response ADDED TO REQUEST $this")
        }

    }

    class ResponseSubmitter(id: Long, scope: ChannelScope, requester: String, source: String) {

        private val packets            = ListBuffer.empty[Packet]
        private val properties         = mutable.HashMap.empty[String, Serializable]
        @volatile private var isSubmit = false

        def addPacket(packet: Packet): this.type = {
            ensureNotSubmit()
            packets += packet
            this
        }

        def addPackets(packets: Packet*): this.type = {
            ensureNotSubmit()
            this.packets ++= packets
            this
        }

        def putProperty(name: String, value: Serializable): this.type = {
            ensureNotSubmit()
            properties.put(name, value)
            this
        }

        def submit(): Unit = {
            ensureNotSubmit()
            val snapshot = Response(id, packets.toArray, properties.toMap, scope.writer.supportIdentifier)
            AppLogger.debug(s"$currentTasksId <> $source: Submitting response ($id)... to $requester")
            scope.sendTo(snapshot, requester)
            AppLogger.debug(s"$currentTasksId <> $source: Response ($id) submited ! ")
            isSubmit = true
        }

        private def ensureNotSubmit(): Unit = {
            if (isSubmit)
                throw new IllegalStateException("Response was already sent." + this)
        }
    }

    case class Response(id: Long,
                        packets: Array[Packet],
                        private val properties: Map[String, Serializable],
                        answerer: String) extends Packet { self =>

        private var packetIndex = 0

        def getProperty(name: String): Serializable = properties(name)

        @throws[NoSuchElementException]("If this method is called more times than packet array's length" + this)
        def nextPacket[P <: Packet : ClassTag](): P = {
            if (packetIndex >= packets.length)
                throw new NoSuchElementException()
            val packet = packets(packetIndex)
            packetIndex += 1
            ensureType[P](packet)
        }

    }

}
