package fr.`override`.linkit.api.packet.collector

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.packet.traffic.{ImmediatePacketInjectable, PacketTraffic}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, PacketFactory}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

class CommunicationPacketCollector(traffic: PacketTraffic, collectorID: Int)
        extends AbstractPacketCollector(traffic, collectorID, false)
                with ImmediatePacketInjectable {

    private val responses: BlockingDeque[Packet] = new LinkedBlockingDeque()
    private val requestListeners = new ConsumerContainer[(Packet, PacketCoordinates)]
    private val normalPacketListeners = new ConsumerContainer[(Packet, PacketCoordinates)]

    var enablePacketSending = true
    var packetTransform: Packet => Packet = p => p

    override def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        def injectAsNormal(): Unit = normalPacketListeners.applyAll((packet, coordinates))

        packet match {
            case wrapped: WrappedPacket =>
                val subPacket = wrapped.subPacket
                wrapped.category match {
                    case "res" => responses.addLast(subPacket)
                    case "req" => requestListeners.applyAll((subPacket, coordinates))
                    case _ => injectAsNormal()
                }
            case _ => injectAsNormal()
        }
    }

    override def addOnPacketInjected(action: (Packet, PacketCoordinates) => Unit): Unit =
        normalPacketListeners += (tuple => action(tuple._1, tuple._2))

    def addRequestListener(action: (Packet, PacketCoordinates) => Unit): Unit =
        requestListeners += (tuple => action(tuple._1, tuple._2))

    def nextResponse[P <: Packet](factory: PacketFactory[P]): P = nextResponse(factory.packetClass)

    def nextResponse[P <: Packet](classOfP: Class[P]): P = nextResponse().asInstanceOf[P]

    def nextResponse(): Packet = responses.takeFirst()

    def broadcastRequest(packet: Packet): Unit = if (enablePacketSending) {
        sendRequest(packetTransform(packet), "BROADCAST")
    }

    def sendRequest(packet: Packet, targetID: String): Unit = if (enablePacketSending) {
        traffic.writePacket(WrappedPacket("req", packetTransform(packet)), identifier, targetID)
    }

    def broadcastResponse(packet: Packet): Unit = if (enablePacketSending) {
        sendResponse(packetTransform(packet), "BROADCAST")
    }

    def sendResponse(packet: Packet, targetID: String): Unit = if (enablePacketSending) {
        traffic.writePacket(WrappedPacket("res", packetTransform(packet)), identifier, targetID)
    }

}

object CommunicationPacketCollector extends PacketCollectorFactory[CommunicationPacketCollector] {
    override val collectorClass: Class[CommunicationPacketCollector] = classOf[CommunicationPacketCollector]

    override def createNew(traffic: PacketTraffic, collectorId: Int): CommunicationPacketCollector =
        new CommunicationPacketCollector(traffic, collectorId)
}