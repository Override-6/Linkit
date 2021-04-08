package fr.linkit.core.connection.packet.serialization

import fr.linkit.api.connection.packet.serialization.{PacketTransferResult, Serializer}
import fr.linkit.api.connection.packet.{Packet, PacketAttributes, PacketCoordinates}
import fr.linkit.core.connection.packet.SimplePacketAttributes
import fr.linkit.core.connection.packet.fundamental.EmptyPacket

import scala.reflect.{ClassTag, classTag}

case class LazyPacketDeserializationResult(override val bytes: Array[Byte],
                                           serializer: () => Serializer) extends PacketTransferResult {

    private lazy  val cache                         = serializer().deserializeAll(bytes)
    override lazy val coords    : PacketCoordinates = extract[PacketCoordinates](null)
    override lazy val attributes: PacketAttributes  = extract[PacketAttributes](SimplePacketAttributes.empty)
    override lazy val packet    : Packet            = extract[Packet](EmptyPacket).prepare()

    private def extract[T <: Serializable : ClassTag](orElse: => T): T = {
        val clazz       = classTag[T].runtimeClass
        val coordsIndex = cache.indexWhere(o => clazz.isAssignableFrom(o.getClass))

        if (coordsIndex < 0) {
            val alternative = orElse
            if (alternative == null)
                throw MalFormedPacketException(bytes, s"Received unknown packet array (${cache.mkString("Array(", ", ", ")")})")
            else return alternative
        }
        cache(coordsIndex) match {
            case e: T => e
        }
    }

}