package fr.`override`.linkit.skull.connection.packet

import fr.`override`.linkit.skull.connection.packet.serialization.ObjectSerializer

trait PacketCoordinates extends Serializable {

    def determineSerializer(array: Array[String], raw: ObjectSerializer, cached: ObjectSerializer): ObjectSerializer

}