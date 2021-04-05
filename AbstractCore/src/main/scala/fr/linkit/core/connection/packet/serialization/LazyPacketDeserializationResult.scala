package fr.linkit.core.connection.packet.serialization

import fr.linkit.api.connection.packet.serialization.{PacketTransferResult, Serializer}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.core.connection.packet.fundamental.EmptyPacket
import fr.linkit.core.connection.packet.{SimplePacketAttributes, UnexpectedPacketException}

case class LazyPacketDeserializationResult(override val bytes: Array[Byte],
                                           serializer: () => Serializer) extends PacketTransferResult {

    private lazy  val cache                         = serializer().deserializeAll(bytes)
    override lazy val coords    : PacketCoordinates = cache(0).asInstanceOf[PacketCoordinates]
    override lazy val attributes: PacketAttributes  = getAttributes
    override lazy val packet    : Packet            = getPacket.prepare()

    private def getAttributes: PacketAttributes = {
        cache match {
            case Array(_: PacketCoordinates, atr: PacketAttributes, _: Packet) =>
                atr

            case Array(_: PacketCoordinates, atr: PacketAttributes) =>
                atr

            case Array(_: PacketCoordinates)
                 | Array(_: PacketCoordinates, _: Packet) =>
                SimplePacketAttributes.empty

            case _ => throw UnexpectedPacketException(s"Received unknown packet info array (${cache.mkString("Array(", ", ", ")")})")
        }
    }

    private def getPacket: Packet = {
        cache match {
            case Array(_: PacketCoordinates, _: PacketAttributes, packet: Packet) =>
                packet

            case Array(_: PacketCoordinates, packet: Packet) =>
                packet

            case Array(_: PacketCoordinates)
                 | Array(_: PacketCoordinates, _: PacketAttributes) =>
                EmptyPacket

            case _ => throw UnexpectedPacketException(s"Received unknown packet info array (${cache.mkString("Array(", ", ", ")")})")
        }
    }

}