package fr.`override`.linkit.api.packet.channel

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.packet.traffic.{ImmediatePacketInjectable, PacketTraffic}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, PacketFactory}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

class CommunicationPacketChannel(override val identifier: Int,
                                 override val connectedID: String,
                                 traffic: PacketTraffic)
        extends PacketChannel(traffic)
                with ImmediatePacketInjectable{

    private val responses: BlockingDeque[Packet] = new LinkedBlockingDeque()
    private val requestListeners = new ConsumerContainer[(Packet, PacketCoordinates)]
    private val normalPacketListeners = new ConsumerContainer[(Packet, PacketCoordinates)]

    var enablePacketSending = true
    var packetTransform: Packet => Packet = p => p

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
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

    def nextResponse[P <: Packet](factory: PacketFactory[P]): P = nextResponse().asInstanceOf[P]

    def nextResponse(): Packet = responses.takeFirst()

    def sendResponse(packet: Packet): Unit = if (enablePacketSending) {
        traffic.writePacket(WrappedPacket("res", packetTransform(packet)), coordinates)
    }

    @deprecated("Use sendRequest or sendResponse instead.")
    override def sendPacket(packet: Packet): Unit = if (enablePacketSending) {
        traffic.writePacket(packetTransform(packet), coordinates)
    }

    def sendRequest(packet: Packet): Unit = if (enablePacketSending) {
        traffic.writePacket(WrappedPacket("req", packetTransform(packet)), coordinates)
    }

}

object CommunicationPacketChannel extends PacketChannelFactory[CommunicationPacketChannel] {
    override val channelClass: Class[CommunicationPacketChannel] = classOf[CommunicationPacketChannel]

    override def createNew(traffic: PacketTraffic, channelId: Int, connectedID: String): CommunicationPacketChannel =
        new CommunicationPacketChannel(channelId, connectedID, traffic)
}
