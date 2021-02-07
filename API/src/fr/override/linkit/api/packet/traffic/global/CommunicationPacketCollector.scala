package fr.`override`.linkit.api.packet.traffic.global

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import fr.`override`.linkit.api.concurrency.RelayWorkerThreadPool
import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCompanion, PacketCoordinates}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

class CommunicationPacketCollector protected(writer: PacketWriter, providable: Boolean)
        extends AbstractPacketCollector(writer, false) {

    private val responses: BlockingQueue[Packet] = {
        if (!providable)
            new LinkedBlockingQueue[Packet]()
        else {
            RelayWorkerThreadPool.ifCurrentOrElse(_.newProvidedQueue, new LinkedBlockingQueue[Packet]())
        }
    }

    private val requestListeners = new ConsumerContainer[(Packet, PacketCoordinates)]
    private val normalPacketListeners = new ConsumerContainer[(Packet, PacketCoordinates)]

    var enablePacketSending = true

    override def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        def injectAsNormal(): Unit = normalPacketListeners.applyAll((packet, coordinates))

        packet match {
            case wrapped: WrappedPacket =>
                val subPacket = wrapped.subPacket
                wrapped.tag match {
                    case "res" => responses.add(subPacket)
                    case "req" => requestListeners.applyAll((subPacket, coordinates))
                    case _ => injectAsNormal()
                }
            case _ => injectAsNormal()
        }
    }

    def addRequestListener(action: (Packet, PacketCoordinates) => Unit): Unit =
        requestListeners += (tuple => action(tuple._1, tuple._2))

    def nextResponse[P <: Packet](factory: PacketCompanion[P]): P = nextResponse(factory.packetClass)

    def nextResponse[P <: Packet](classOfP: Class[P]): P = nextResponse().asInstanceOf[P]

    def nextResponse(): Packet = responses.take()

    def broadcastRequest(packet: Packet): Unit = if (enablePacketSending) {
        sendRequest(packet, "BROADCAST")
    }

    def sendRequest(packet: Packet, targetID: String): Unit = if (enablePacketSending) {
        writer.writePacket(WrappedPacket("req", packet), targetID)
    }

    def broadcastResponse(packet: Packet): Unit = if (enablePacketSending) {
        sendResponse(packet, "BROADCAST")
    }

    def sendResponse(packet: Packet, targetID: String): Unit = if (enablePacketSending) {
        writer.writePacket(WrappedPacket("res", packet), targetID)
    }

}

object CommunicationPacketCollector extends PacketCollectorFactory[CommunicationPacketCollector] {

    override def createNew(writer: PacketWriter): CommunicationPacketCollector = {
        new CommunicationPacketCollector(writer, false)
    }

    def providable: PacketCollectorFactory[CommunicationPacketCollector] = {
        new CommunicationPacketCollector(_, true)
    }
}
