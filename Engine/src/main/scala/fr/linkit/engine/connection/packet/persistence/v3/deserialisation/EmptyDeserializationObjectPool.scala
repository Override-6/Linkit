package fr.linkit.engine.connection.packet.persistence.v3.deserialisation

import fr.linkit.api.connection.packet.persistence.v3.deserialisation.{DeserializationObjectPool, DeserializationProgression}
import fr.linkit.api.connection.packet.persistence.v3.deserialisation.node.DeserializerNode

object EmptyDeserializationObjectPool extends DeserializationObjectPool {
    override def initPool(progression: DeserializationProgression): Unit = ()

    override def getHeaderValueNode(place: Int): DeserializerNode =
        throw new NoSuchElementException()
}
