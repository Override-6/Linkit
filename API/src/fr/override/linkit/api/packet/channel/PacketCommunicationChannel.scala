package fr.`override`.linkit.api.packet.channel

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.packet.traffic.{ImmediatePacketInjectable, PacketWriter}
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, PacketFactory}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

class PacketCommunicationChannel(override val identifier: Int,
                                 override val connectedID: String,
                                 sender: PacketWriter)
        extends PacketChannel(sender)
                with ImmediatePacketInjectable{

    private val responses: BlockingDeque[Packet] = new LinkedBlockingDeque()
    private val requestListeners = new ConsumerContainer[(Packet, PacketCoordinates)]
    private val normalPacketListeners = new ConsumerContainer[(Packet, PacketCoordinates)]

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

    def nextResponse[P <: Packet](factory: PacketFactory[P]): P = nextResponse(factory.packetClass)

    def nextResponse[P <: Packet](classOfP: Class[P]): P = nextResponse().asInstanceOf[P]

    def nextResponse(): Packet = responses.takeFirst()

    def sendResponse(packet: Packet): Unit = sender.writePacket(WrappedPacket("res", packet), coordinates)

    @deprecated("Use sendRequest or sendResponse instead.")
    override def sendPacket(packet: Packet): Unit = sendRequest(packet)

    def sendRequest(packet: Packet): Unit = sender.writePacket(WrappedPacket("req", packet), coordinates)

}

object PacketCommunicationChannel extends PacketChannelFactory[PacketCommunicationChannel] {
    override val channelClass: Class[PacketCommunicationChannel] = classOf[PacketCommunicationChannel]

    override def createNew(writer: PacketWriter, channelId: Int, connectedID: String): PacketCommunicationChannel =
        new PacketCommunicationChannel(channelId, connectedID, writer)
}
