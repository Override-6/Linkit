package fr.`override`.linkit.api.packet.collector

import java.util.concurrent.{BlockingDeque, LinkedBlockingDeque}

import fr.`override`.linkit.api.exception.UnexpectedPacketException
import fr.`override`.linkit.api.packet.traffic.PacketWriter
import fr.`override`.linkit.api.packet.{Packet, PacketCoordinates, PacketFactory}
import fr.`override`.linkit.api.utils.{ConsumerContainer, WrappedPacket}

class ReqAndRespPacketCollector(writer: PacketWriter, collectorID: Int) extends AbstractPacketCollector(writer, collectorID, false) {
    private val responses: BlockingDeque[Packet] = new LinkedBlockingDeque()
    private val requestListeners = new ConsumerContainer[(Packet, PacketCoordinates)]

    override def handlePacket(packet: Packet, coordinates: PacketCoordinates): Unit = {
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

    def sendRequest(packet: Packet, targetID: String): Unit = writer.writePacket(WrappedPacket("req", packet), identifier, targetID)

    @deprecated("Use sendRequest or sendResponse instead.")
    override def sendPacket(packet: Packet, targetID: String): Unit = sendResponse(packet, targetID)

    def sendResponse(packet: Packet, targetID: String): Unit = writer.writePacket(WrappedPacket("res", packet), identifier, targetID)


}

object ReqAndRespPacketCollector extends PacketCollectorFactory[ReqAndRespPacketCollector] {
    override val collectorClass: Class[ReqAndRespPacketCollector] = classOf[ReqAndRespPacketCollector]

    override def createNew(writer: PacketWriter, collectorId: Int): ReqAndRespPacketCollector =
        new ReqAndRespPacketCollector(writer, collectorId)
}
