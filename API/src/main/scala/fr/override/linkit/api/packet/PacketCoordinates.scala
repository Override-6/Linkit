package fr.`override`.linkit.api.packet

import fr.`override`.linkit.api.packet.serialization.ObjectSerializer

trait PacketCoordinates extends Serializable {

    def determineSerializer(array: Array[String], raw: ObjectSerializer, cached: ObjectSerializer): ObjectSerializer

}