package fr.linkit.engine.connection.packet.persistence.v3.serialisation.node

import fr.linkit.api.connection.packet.persistence.v3.serialisation.SerialisationOutputStream
import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.{ObjectSerializerNode, SerializerNode}
import fr.linkit.engine.connection.packet.persistence.v3.serialisation.SerializerNodeFlags.ObjectFlag

case class SimpleObjectSerializerNode(serializer: SerializerNode) extends ObjectSerializerNode {
    override def writeBytes(out: SerialisationOutputStream): Unit = {
        if (out.position() == 0 || out.get(out.position() - 1) != ObjectFlag)
            out.put(ObjectFlag)
        serializer.writeBytes(out)
    }
}
