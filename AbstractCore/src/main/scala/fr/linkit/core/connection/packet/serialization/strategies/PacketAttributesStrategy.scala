package fr.linkit.core.connection.packet.serialization.strategies

import fr.linkit.api.connection.packet.serialization.strategy.{SerialStrategy, StrategicSerializer}
import fr.linkit.core.connection.packet.SimplePacketAttributes

object PacketAttributesStrategy extends SerialStrategy[SimplePacketAttributes] {

    override def getTypeHandling: Class[SimplePacketAttributes] = classOf[SimplePacketAttributes]

    override def serial(instance: SimplePacketAttributes, serializer: StrategicSerializer): Array[Byte] = {
        val tuples = instance.attributes.toArray
        serializer.serialize(tuples, false)
    }

    override def deserial(bytes: Array[Byte], serializer: StrategicSerializer): Any = {
        val tuples = serializer.deserializeObject(bytes, classOf[Array[(String, Serializable)]])
        SimplePacketAttributes.from(tuples: _*)
    }
}
