package fr.`override`.linkit.api.packet.channel

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, PacketFactory}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

class ReqAndRespPacketChannel(override val identifier: Int,
                              override val connectedID: String,
                              sender: PacketWriter) extends PacketChannel(sender) {

    private val responses: BlockingDeque[Packet] = new LinkedBlockingDeque()
    private val requestListeners = new ConsumerContainer[(Packet, PacketCoordinates)]

    override def injectPacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
        packet match {
            case wrapped: WrappedPacket =>
                val subPacket = wrapped.subPacket
                wrapped.category match {
                    case "res" => responses.addLast(subPacket)
                    case "req" => requestListeners.applyAll((subPacket, coordinates))
                }
            case _ => throw new UnexpectedPacketException("Attempted to inject a non-Wrapped Packet into this ReqAndRespPacketChannel")
        }
    }

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

object ReqAndRespPacketChannel extends PacketChannelFactory[ReqAndRespPacketChannel] {
    override val channelClass: Class[ReqAndRespPacketChannel] = classOf[ReqAndRespPacketChannel]

    override def createNew(writer: PacketWriter, channelId: Int, connectedID: String): ReqAndRespPacketChannel =
        new ReqAndRespPacketChannel(channelId, connectedID, writer)
}
