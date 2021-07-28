package fr.linkit.engine.connection.packet.persistence.v3.serialisation

import fr.linkit.api.connection.packet.persistence.v3.serialisation.node.{DelegatingSerializerNode, SerializerNode}
import fr.linkit.api.connection.packet.persistence.v3.serialisation.{SerialisationOutputStream, SerializationObjectPool}

object FakeSerializationObjectPool extends SerializationObjectPool {
    override def checkNode(obj: Any, out: SerialisationOutputStream)(node: => SerializerNode): DelegatingSerializerNode = DelegatingSerializerNode(node)

    override def containsInstance(obj: Any): Boolean = false

    override def addSerialisationDepth(): Unit = ()

    override def removeSerialisationDepth(): Unit = ()
}
