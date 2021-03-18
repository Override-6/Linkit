package fr.`override`.linkit.api.connection.packet

import fr.`override`.linkit.api.connection.packet.serialization.Serializer

trait PacketCoordinates extends Serializable {

    def determineSerializer(array: Array[String], raw: Serializer, cached: Serializer): Serializer

}